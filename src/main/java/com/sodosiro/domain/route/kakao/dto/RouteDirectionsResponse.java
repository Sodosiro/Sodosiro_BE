package com.sodosiro.domain.route.kakao.dto;

import java.util.List;

public record RouteDirectionsResponse(
        List<RouteLeg> legs
) {
    public static RouteDirectionsResponse from(List<RouteLeg> legs) {
        return new RouteDirectionsResponse(legs);
    }
}