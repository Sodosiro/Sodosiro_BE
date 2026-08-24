package com.sodosiro.domain.like.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record MyLikedSpotListResponse(
        List<MyLikedSpotItem> content,
        String nextCursor,
        boolean hasNext,

        @Schema(description = "필터(sigunguCode, category) 조건을 만족하는 좋아요한 관광지 총 개수 (커서와 무관)", example = "37")
        long totalCount
) {
}
