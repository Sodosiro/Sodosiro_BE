package com.sodosiro.domain.review.controller.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;

public record ReviewListResponse(
        long totalCount,
        @Schema(description = "리뷰 평균 별점 (소수점 한 자리)", example = "4.5")
        BigDecimal avgRating,
        @Schema(description = "로그인 유저가 해당 관광지에 작성한 리뷰 ID. 작성하지 않았거나 비로그인 시 null", example = "42")
        Long myReviewId,
        List<ReviewResponse> reviews,
        Long nextCursor,
        boolean hasNext
) {
}
