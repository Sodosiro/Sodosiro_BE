package com.sodosiro.domain.route.kakao.dto;

import java.util.List;

public record KakaoTransitRoute(
        KakaoTransitRouteProperties properties,
        List<KakaoTransitStep> steps
) {
}
