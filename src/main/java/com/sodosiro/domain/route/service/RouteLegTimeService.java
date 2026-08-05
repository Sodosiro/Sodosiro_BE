package com.sodosiro.domain.route.service;

import com.sodosiro.domain.route.RouteSearchClient;
import com.sodosiro.domain.route.RouteSearchClientFactory;
import com.sodosiro.domain.route.dto.RouteLeg;
import com.sodosiro.domain.route.dto.RouteWaypoint;
import com.sodosiro.domain.route.dto.TransportMode;
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

    private final RouteSearchClientFactory routeSearchClientFactory;

    public List<RouteLeg> calculateAdjacentLegTimes(List<RouteWaypoint> orderedWaypoints, TransportMode mode) {
        if (orderedWaypoints.size() < 2) {
            return List.of();
        }

        RouteSearchClient routeSearchClient = routeSearchClientFactory.getClient(mode);

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<RouteLeg>> futures = new ArrayList<>();

            for (int i = 0; i < orderedWaypoints.size() - 1; i++) {
                RouteWaypoint from = orderedWaypoints.get(i);
                RouteWaypoint to = orderedWaypoints.get(i + 1);

                CompletableFuture<RouteLeg> future = CompletableFuture
                        .supplyAsync(() -> routeSearchClient.findRoute(from, to), executor)
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
}
