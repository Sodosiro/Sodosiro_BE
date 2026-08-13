package com.sodosiro.domain.review.controller.dto.response;

import java.util.List;

public record MyReviewListResponse(
        List<ReviewResponse> reviews,
        Long nextCursor,
        boolean hasNext
) { }
