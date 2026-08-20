package com.sodosiro.domain.route.kakao.dto;

import com.sodosiro.domain.route.dto.RouteCoordinate;

import java.util.List;

public record DirectionsLegResult(boolean success, Long durationSeconds, Long distanceMeters, List<RouteCoordinate> path) {

    public static DirectionsLegResult success(long durationSeconds, long distanceMeters, List<RouteCoordinate> path) {
        return new DirectionsLegResult(true, durationSeconds, distanceMeters, path);
    }

    public static DirectionsLegResult failure() {
        return new DirectionsLegResult(false, null, null, List.of());
    }
}
