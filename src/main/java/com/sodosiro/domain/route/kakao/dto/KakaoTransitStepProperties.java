package com.sodosiro.domain.route.kakao.dto;

import java.util.List;

/** type: BUS / SUBWAY / WALKING */
public record KakaoTransitStepProperties(
        String guidance,
        String type,
        Integer distance,
        Integer time,
        List<KakaoTransitStop> stops,
        List<KakaoTransitVehicle> vehicles
) {
}
