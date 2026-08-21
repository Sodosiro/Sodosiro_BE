package com.sodosiro.domain.route.kakao.dto;

import java.util.List;

/** GET https://dapi.kakao.com/v2/routing/publictraffic 원본 응답 */
public record KakaoTransitResponse(
        String status,
        KakaoTransitProperties properties,
        List<KakaoTransitRoute> routes
) {
}
