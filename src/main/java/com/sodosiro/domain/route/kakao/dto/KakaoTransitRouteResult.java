package com.sodosiro.domain.route.kakao.dto;

import java.util.List;

/** 카카오 대중교통 길찾기 결과. 지도에 단계별로 다른 색 경로선을 그릴 수 있도록 단계별 상세(steps)를 내려준다. */
public record KakaoTransitRouteResult(
        boolean success,
        String type,
        Integer totalTimeSeconds,
        Integer totalDistanceMeters,
        Integer transfers,
        Integer fare,
        List<KakaoTransitStepResult> steps
) {

    public static KakaoTransitRouteResult success(
            String type,
            Integer totalTimeSeconds,
            Integer totalDistanceMeters,
            Integer transfers,
            Integer fare,
            List<KakaoTransitStepResult> steps
    ) {
        return new KakaoTransitRouteResult(true, type, totalTimeSeconds, totalDistanceMeters, transfers, fare, steps);
    }

    public static KakaoTransitRouteResult failure() {
        return new KakaoTransitRouteResult(false, null, null, null, null, null, List.of());
    }
}
