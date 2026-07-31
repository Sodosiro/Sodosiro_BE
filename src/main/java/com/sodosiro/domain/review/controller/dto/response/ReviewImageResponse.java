package com.sodosiro.domain.review.controller.dto.response;

import com.sodosiro.domain.review.entity.ReviewImage;

public record ReviewImageResponse(
        String imageUrl,
        Integer displayOrder
) {
    /**
     * Creates a response from a review image.
     *
     * @param image the review image to convert
     * @return a response containing the image URL and display order
     */
    public static ReviewImageResponse from(ReviewImage image) {
        return new ReviewImageResponse(image.getImageUrl(), image.getDisplayOrder());
    }
}
