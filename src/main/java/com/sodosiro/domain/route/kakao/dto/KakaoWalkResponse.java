package com.sodosiro.domain.route.kakao.dto;

/** GET https://dapi.kakao.com/v2/routing/walk 원본 응답 */
public record KakaoWalkResponse(String status, KakaoWalkRoute route) {
}
