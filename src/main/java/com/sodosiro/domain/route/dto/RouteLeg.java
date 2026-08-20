package com.sodosiro.domain.route.dto;

import java.util.List;

public record RouteLeg(Long fromId, Long toId, Long durationSeconds, Long distanceMeters, List<RouteCoordinate> path, boolean success) {

    public static RouteLeg success(
            Long fromId, Long toId, Long durationSeconds, Long distanceMeters, List<RouteCoordinate> path) {
        return new RouteLeg(fromId, toId, durationSeconds, distanceMeters, path, true);
    }

    public static RouteLeg failure(Long fromId, Long toId) {
        return new RouteLeg(fromId, toId, null, null, List.of(), false);
    }
}
