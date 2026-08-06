package com.sodosiro.domain.route.kakao.dto;

public record DirectionsLegResult(boolean success, Long durationSeconds, Long distanceMeters) {

    public static DirectionsLegResult success(long durationSeconds, long distanceMeters) {
        return new DirectionsLegResult(true, durationSeconds, distanceMeters);
    }

    public static DirectionsLegResult failure() {
        return new DirectionsLegResult(false, null, null);
    }
}
