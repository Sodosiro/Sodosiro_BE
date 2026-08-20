package com.sodosiro.domain.route.kakao.dto;

import com.sodosiro.domain.route.dto.RouteCoordinate;

import java.util.List;

/** 대중교통 경로 단계(도보/버스/지하철) 요약. 지도에 단계별로 다른 색 경로선을 그릴 수 있도록 path를 단계별로도 제공한다. */
public record KakaoTransitStepResult(
        String type,
        String guidance,
        Integer distanceMeters,
        Integer timeSeconds,
        List<String> stopNames,
        List<String> vehicleNames,
        List<RouteCoordinate> path
) {
}
