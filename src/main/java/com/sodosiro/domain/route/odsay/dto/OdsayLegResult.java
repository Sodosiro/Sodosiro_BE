package com.sodosiro.domain.route.odsay.dto;

public record OdsayLegResult(boolean success, Long durationSeconds, Long distanceMeters, Integer payment) {

    public static OdsayLegResult success(long durationMinutes, long distanceMeters, int payment) {
        return new OdsayLegResult(true, durationMinutes * 60, distanceMeters, payment);
    }

    public static OdsayLegResult failure() {
        return new OdsayLegResult(false, null, null, null);
    }
}