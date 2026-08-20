package com.sodosiro.domain.route.kakao.dto;

import com.sodosiro.domain.route.dto.RouteCoordinate;

import java.util.List;

/** 카카오 대중교통 길찾기 결과. 프론트 지도 표시를 위해 전체 구간 좌표(path)와 단계별 상세(steps)를 함께 내려준다. */
public record KakaoTransitRouteResult(
        boolean success,
        String type,
        Integer totalTimeSeconds,
        Integer totalDistanceMeters,
        Integer transfers,
        Integer fare,
        List<RouteCoordinate> path,
        List<KakaoTransitStepResult> steps
) {

    public static KakaoTransitRouteResult success(
            String type,
            Integer totalTimeSeconds,
            Integer totalDistanceMeters,
            Integer transfers,
            Integer fare,
            List<RouteCoordinate> path,
            List<KakaoTransitStepResult> steps
    ) {
        return new KakaoTransitRouteResult(true, type, totalTimeSeconds, totalDistanceMeters, transfers, fare, path, steps);
    }

    public static KakaoTransitRouteResult failure() {
        return new KakaoTransitRouteResult(false, null, null, null, null, null, List.of(), List.of());
    }
}
