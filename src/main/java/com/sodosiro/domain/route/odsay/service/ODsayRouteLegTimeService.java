package com.sodosiro.domain.route.odsay.service;

import com.sodosiro.domain.route.RouteSearchClient;
import com.sodosiro.domain.route.dto.RouteLeg;
import com.sodosiro.domain.route.dto.RouteWaypoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Service
public class ODsayRouteLegTimeService {

    private final RouteSearchClient routeSearchClient;

    public ODsayRouteLegTimeService(@Qualifier("odsayDirectionsClient") RouteSearchClient routeSearchClient) {
        this.routeSearchClient = routeSearchClient;
    }

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