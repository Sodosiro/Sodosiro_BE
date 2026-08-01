package com.sodosiro.domain.route.kakao.service;

import com.sodosiro.domain.route.kakao.client.KakaoDirectionsClient;
import com.sodosiro.domain.route.kakao.dto.DirectionsLegResult;
import com.sodosiro.domain.route.kakao.dto.RouteLeg;
import com.sodosiro.domain.route.kakao.dto.RouteWaypoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RouteLegTimeService {

    private final KakaoDirectionsClient kakaoDirectionsClient;

    public List<RouteLeg> calculateAdjacentLegTimes(List<RouteWaypoint> orderedWaypoints) {
        if (orderedWaypoints.size() < 2) {
            return List.of();
        }

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<RouteLeg>> futures = new ArrayList<>();

            for (int i = 0; i < orderedWaypoints.size() - 1; i++) {
                RouteWaypoint from = orderedWaypoints.get(i);
                RouteWaypoint to = orderedWaypoints.get(i + 1);

                CompletableFuture<RouteLeg> future = CompletableFuture
                        .supplyAsync(() -> toRouteLeg(from, to), executor)
                        .exceptionally(throwable -> {
                            log.warn("구간 이동시간 계산 실패: from={}, to={}", from.id(), to.id(), throwable);
                            return RouteLeg.failure(from.id(), to.id());
                        });

                futures.add(future);
            }

            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        }
    }

    private RouteLeg toRouteLeg(RouteWaypoint from, RouteWaypoint to) {
        DirectionsLegResult result = kakaoDirectionsClient.findRoute(from, to);
        if (!result.success()) {
            return RouteLeg.failure(from.id(), to.id());
        }
        return RouteLeg.success(from.id(), to.id(), result.durationSeconds(), result.distanceMeters());
    }
}
