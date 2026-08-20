package com.sodosiro.domain.route.service;

import com.sodosiro.domain.route.dto.RouteWaypoint;
import com.sodosiro.domain.route.kakao.client.KakaoTransitDirectionsClient;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitRouteResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** 대중교통(Kakao) 전용 구간별 상세 경로 계산(버스/지하철/도보 구간, 좌표 포함) */
@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoTransitRouteService {

    private final KakaoTransitDirectionsClient kakaoTransitDirectionsClient;

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
                        .supplyAsync(() -> kakaoTransitDirectionsClient.findRouteDetail(from, to), executor)
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
}
