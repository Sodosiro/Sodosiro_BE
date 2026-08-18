package com.sodosiro.domain.travel.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record TrendingKeywordResponse(
        @Schema(description = "순위 (1부터)", example = "1")
        int rank,
        @Schema(description = "검색어", example = "강릉")
        String keyword,
        @Schema(description = "누적 검색 횟수", example = "128")
        long count
) {
    public static TrendingKeywordResponse of(int rank, String keyword, long count) {
        return new TrendingKeywordResponse(rank, keyword, count);
    }
}
