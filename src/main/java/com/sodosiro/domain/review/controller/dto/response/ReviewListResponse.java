package com.sodosiro.domain.review.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record ReviewListResponse(
        long totalCount,
        @Schema(description = "리뷰 평균 별점 (소수점 한 자리)", example = "4.5")
        BigDecimal avgRating,
        List<ReviewResponse> reviews,
        Long nextCursor,
        boolean hasNext
) {
}
