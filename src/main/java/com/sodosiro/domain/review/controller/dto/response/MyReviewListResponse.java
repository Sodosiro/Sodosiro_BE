package com.sodosiro.domain.review.controller.dto.response;

import com.sodosiro.domain.review.entity.Review;
import com.sodosiro.domain.review.entity.ReviewImage;
import com.sodosiro.domain.travel.entity.TouristSpot;

import java.time.LocalDateTime;
import java.util.List;

public record MyReviewListResponse(
        List<MyReviewResponse> reviews,
        Long nextCursor,
        boolean hasNext
) {
    public record MyReviewResponse(
            Long reviewId,
            SpotSummary spot,
            Short rating,
            String body,
            List<ReviewImageResponse> images,
            LocalDateTime createdAt
    ) {
        public record SpotSummary(Long contentId, String title, String firstImage) {
            /**
             * Creates a spot summary from a tourist spot.
             *
             * @param spot the tourist spot to summarize
             * @return a summary containing the spot's content ID, title, and first image
             */
            public static SpotSummary from(TouristSpot spot) {
                return new SpotSummary(spot.getContentId(), spot.getTitle(), spot.getFirstImage());
            }
        }

        /**
         * Creates a review response from a review, its associated tourist spot, and images.
         *
         * @param review the review entity
         * @param spot the tourist spot associated with the review
         * @param images the images associated with the review
         * @return a response containing the review details and spot summary
         */
        public static MyReviewResponse of(Review review, TouristSpot spot, List<ReviewImage> images) {
            return new MyReviewResponse(
                    review.getId(),
                    SpotSummary.from(spot),
                    review.getRating(),
                    review.getBody(),
                    images.stream().map(ReviewImageResponse::from).toList(),
                    review.getCreatedAt()
            );
        }
    }
}
