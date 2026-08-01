package com.sodosiro.domain.route.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record KakaoRoute(
        @JsonProperty("result_code") int resultCode,
        KakaoRouteSummary summary
) {
}
