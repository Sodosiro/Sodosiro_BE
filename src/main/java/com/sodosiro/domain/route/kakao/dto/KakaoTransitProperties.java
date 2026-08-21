package com.sodosiro.domain.route.kakao.dto;

public record KakaoTransitProperties(
        Integer total,
        Integer bus,
        Integer subway,
        Integer busAndSubway,
        String landingURL
) {
}
