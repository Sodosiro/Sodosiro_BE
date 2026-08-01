package com.sodosiro.domain.route.odsay.dto;

import java.util.List;

public record OdsayRouteDetailResponse(
        boolean success,
        Integer totalTimeMinutes,
        Integer totalDistanceMeters,
        Integer payment,
        List<OdsaySegmentResponse> segments
) {

    public static OdsayRouteDetailResponse success(
            Integer totalTimeMinutes,
            Integer totalDistanceMeters,
            Integer payment,
            List<OdsaySegmentResponse> segments
    ) {
        return new OdsayRouteDetailResponse(true, totalTimeMinutes, totalDistanceMeters, payment, segments);
    }

    public static OdsayRouteDetailResponse failure() {
        return new OdsayRouteDetailResponse(false, null, null, null, List.of());
    }
}
