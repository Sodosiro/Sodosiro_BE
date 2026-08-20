package com.sodosiro.domain.route.kakao.dto;

public record KakaoTransitStep(
        KakaoTransitStepProperties properties,
        KakaoTransitPath path
) {
}
