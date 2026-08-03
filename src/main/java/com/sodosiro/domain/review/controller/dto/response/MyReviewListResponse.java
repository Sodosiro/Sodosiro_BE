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
        public record SpotSummary(Long contentId, String kakaoContentId, String title, String firstImage) {
            public static SpotSummary from(TouristSpot spot) {
                return new SpotSummary(
                        spot.getContentId(), spot.getKakaoContentId(), spot.getTitle(), spot.getFirstImage());
            }
        }

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
