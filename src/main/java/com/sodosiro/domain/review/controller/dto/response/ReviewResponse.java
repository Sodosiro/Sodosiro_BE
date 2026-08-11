package com.sodosiro.domain.review.controller.dto.response;

import com.sodosiro.domain.review.entity.Review;
import com.sodosiro.domain.review.entity.ReviewImage;
import com.sodosiro.domain.review.constants.ReviewVisitType;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.user.entity.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
        Long reviewId,
        AuthorInfo author,
        SpotSummary spot,
        BigDecimal rating,
        String body,
        List<ReviewImageResponse> images,
        LocalDateTime createdAt,
        ReviewVisitType visitType,
        boolean gpsVerified,
        LocalDateTime gpsVerifiedAt,
        boolean isMyReview
) {
    public record AuthorInfo(Long userId, String displayName, String profileImageUrl) {
        public static AuthorInfo from(User user) {
            return new AuthorInfo(user.getUserId(), user.getDisplayName(), user.getProfileImageUrl());
        }
    }

    public record SpotSummary(Long contentId, String title, String firstImage) {
        public static SpotSummary from(TouristSpot spot) {
            if (spot == null) {
                return null;
            }
            return new SpotSummary(spot.getContentId(), spot.getTitle(), spot.getFirstImage());
        }
    }

    public static ReviewResponse of(
            Review review, User author, TouristSpot spot, List<ReviewImage> images, Long loginUserId) {
        return new ReviewResponse(
                review.getId(),
                AuthorInfo.from(author),
                SpotSummary.from(spot),
                review.getRating(),
                review.getBody(),
                images.stream().map(ReviewImageResponse::from).toList(),
                review.getCreatedAt(),
                review.getVisitType(),
                review.getVisitType() == ReviewVisitType.GPS_VERIFIED,
                review.getGpsVerifiedAt(),
                review.getUserId().equals(loginUserId)
        );
    }
}
