package com.sodosiro.domain.route.kakao.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoRoute(
        @JsonProperty("result_code") int resultCode,
        KakaoRouteSummary summary
) {
}
