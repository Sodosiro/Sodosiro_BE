package com.sodosiro.domain.route.service;

import com.sodosiro.domain.route.dto.RouteCoordinate;
import com.sodosiro.domain.route.dto.RouteWaypoint;
import com.sodosiro.domain.route.kakao.client.KakaoTransitDirectionsClient;
import com.sodosiro.domain.route.kakao.client.KakaoWalkDirectionsClient;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitRouteResult;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitStepResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

/**
 * 대중교통(Kakao) 전용 구간별 상세 경로 계산.
 * publictraffic API는 버스/지하철 구간만 주기 때문에, 출발지→탑승 정류장 / 하차 정류장→도착지
 * 도보 구간을 walk API로 별도 조회해 앞뒤로 붙인다 (환승 중간 도보는 스코프 밖).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoTransitRouteService {

    private final KakaoTransitDirectionsClient kakaoTransitDirectionsClient;
    private final KakaoWalkDirectionsClient kakaoWalkDirectionsClient;

    public List<KakaoTransitRouteResult> calculateAdjacentRouteDetails(List<RouteWaypoint> orderedWaypoints) {
        if (orderedWaypoints.size() < 2) {
            return List.of();
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<KakaoTransitRouteResult>> futures = new ArrayList<>();

            for (int i = 0; i < orderedWaypoints.size() - 1; i++) {
                RouteWaypoint from = orderedWaypoints.get(i);
                RouteWaypoint to = orderedWaypoints.get(i + 1);

                CompletableFuture<KakaoTransitRouteResult> future = CompletableFuture
                        .supplyAsync(() -> buildFullRouteDetail(from, to), executor)
                        .exceptionally(throwable -> {
                            log.warn("대중교통 구간 상세 계산 실패: from={}, to={}", from.id(), to.id(), throwable);
                            return KakaoTransitRouteResult.failure();
                        });

                futures.add(future);
            }

            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        }
    }

    /** 대중교통 구간을 조회한 뒤, 앞뒤 도보 구간을 채워서 도보-버스(지하철)-도보 순서의 전체 경로로 합친다. */
    private KakaoTransitRouteResult buildFullRouteDetail(RouteWaypoint from, RouteWaypoint to) {
        KakaoTransitRouteResult transitResult = kakaoTransitDirectionsClient.findRouteDetail(from, to);

        Optional<RouteCoordinate> boardingStop = firstCoordinate(transitResult.steps());
        Optional<RouteCoordinate> alightingStop = lastCoordinate(transitResult.steps());
        if (!transitResult.success() || boardingStop.isEmpty() || alightingStop.isEmpty()) {
            return transitResult;
        }

        List<KakaoTransitStepResult> walkToStop = kakaoWalkDirectionsClient.findWalkSteps(
                from.x(), from.y(), boardingStop.get().longitude(), boardingStop.get().latitude());
        List<KakaoTransitStepResult> walkFromStop = kakaoWalkDirectionsClient.findWalkSteps(
                alightingStop.get().longitude(), alightingStop.get().latitude(), to.x(), to.y());

        List<KakaoTransitStepResult> mergedSteps = Stream.of(walkToStop, transitResult.steps(), walkFromStop)
                .flatMap(List::stream)
                .toList();

        return KakaoTransitRouteResult.success(
                transitResult.type(),
                sumTimeSeconds(mergedSteps),
                sumDistanceMeters(mergedSteps),
                transitResult.transfers(),
                transitResult.fare(),
                mergedSteps
        );
    }

    private static Optional<RouteCoordinate> firstCoordinate(List<KakaoTransitStepResult> steps) {
        return steps.stream().flatMap(step -> step.path().stream()).findFirst();
    }

    private static Optional<RouteCoordinate> lastCoordinate(List<KakaoTransitStepResult> steps) {
        List<RouteCoordinate> coordinates = steps.stream().flatMap(step -> step.path().stream()).toList();
        return coordinates.isEmpty() ? Optional.empty() : Optional.of(coordinates.getLast());
    }

    private static int sumTimeSeconds(List<KakaoTransitStepResult> steps) {
        return steps.stream().mapToInt(step -> step.timeSeconds() == null ? 0 : step.timeSeconds()).sum();
    }

    private static int sumDistanceMeters(List<KakaoTransitStepResult> steps) {
        return steps.stream().mapToInt(step -> step.distanceMeters() == null ? 0 : step.distanceMeters()).sum();
    }
}
