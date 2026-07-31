package com.sodosiro.domain.review.controller.dto.response;

import com.sodosiro.domain.review.entity.ReviewImage;

public record ReviewImageResponse(
        String imageUrl,
        Integer displayOrder
) {
    public static ReviewImageResponse from(ReviewImage image) {
        return new ReviewImageResponse(image.getImageUrl(), image.getDisplayOrder());
    }
}
