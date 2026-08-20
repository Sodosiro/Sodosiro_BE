package com.sodosiro.domain.route.kakao.dto;

/** type: BUS / SUBWAY / BUS_AND_SUBWAY */
public record KakaoTransitRouteProperties(
        String type,
        Integer totalDistance,
        Integer totalTime,
        Integer transfers,
        KakaoTransitFare fare
) {
}
