package com.sodosiro.domain.review.controller.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record ReviewListResponse(
        long totalCount,
        BigDecimal avgRating,
        List<ReviewResponse> reviews,
        Long nextCursor,
        boolean hasNext
) {
}
