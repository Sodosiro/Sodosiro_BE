package com.sodosiro.domain.route.odsay.dto;

import java.util.List;

public record OdsayRouteDetailResponse(
        boolean success,
        Integer totalTimeMinutes,
        Integer totalDistanceMeters,
        Integer payment,
        String mapObj,
        List<OdsayCoordinateResponse> path,
        List<OdsaySegmentResponse> segments
) {

    public static OdsayRouteDetailResponse success(
            Integer totalTimeMinutes,
            Integer totalDistanceMeters,
            Integer payment,
            String mapObj,
            List<OdsayCoordinateResponse> path,
            List<OdsaySegmentResponse> segments
    ) {
        return new OdsayRouteDetailResponse(true, totalTimeMinutes, totalDistanceMeters, payment, mapObj, path, segments);
    }

    public static OdsayRouteDetailResponse failure() {
        return new OdsayRouteDetailResponse(false, null, null, null, null, List.of(), List.of());
    }
}
