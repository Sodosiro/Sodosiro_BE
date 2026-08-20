package com.sodosiro.domain.route.dto;

import java.util.List;

public record RouteLeg(
        Long fromId,
        Long toId,
        Long durationSeconds,
        Long distanceMeters,
        Long tollFare,
        Long estimatedFuelCost,
        List<RouteCoordinate> path,
        boolean success) {

    public static RouteLeg success(
            Long fromId,
            Long toId,
            Long durationSeconds,
            Long distanceMeters,
            Long tollFare,
            Long estimatedFuelCost,
            List<RouteCoordinate> path) {
        return new RouteLeg(fromId, toId, durationSeconds, distanceMeters, tollFare, estimatedFuelCost, path, true);
    }

    public static RouteLeg failure(Long fromId, Long toId) {
        return new RouteLeg(fromId, toId, null, null, null, null, List.of(), false);
    }
}
