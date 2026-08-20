package com.sodosiro.domain.route.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record KakaoRoute(
        @JsonProperty("result_code") int resultCode,
        KakaoRouteSummary summary,
        List<KakaoSection> sections
) {
}
