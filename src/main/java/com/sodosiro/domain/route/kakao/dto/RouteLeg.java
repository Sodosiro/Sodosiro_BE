package com.sodosiro.domain.route.kakao.dto;

public record RouteLeg(Long fromId, Long toId, Long durationSeconds, Long distanceMeters, boolean success) {

    public static RouteLeg success(Long fromId, Long toId, Long durationSeconds, Long distanceMeters) {
        return new RouteLeg(fromId, toId, durationSeconds, distanceMeters, true);
    }

    public static RouteLeg failure(Long fromId, Long toId) {
        return new RouteLeg(fromId, toId, null, null, false);
    }
}
