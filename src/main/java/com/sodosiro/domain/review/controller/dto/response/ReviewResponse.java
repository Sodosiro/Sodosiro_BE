package com.sodosiro.domain.review.controller.dto.response;

import com.sodosiro.domain.review.entity.Review;
import com.sodosiro.domain.review.entity.ReviewImage;
import com.sodosiro.domain.review.constants.ReviewVisitType;
import com.sodosiro.domain.user.entity.User;

import java.time.LocalDateTime;
import java.util.List;

public record ReviewResponse(
        Long reviewId,
        AuthorInfo author,
        Short rating,
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

    public static ReviewResponse of(Review review, User author, List<ReviewImage> images, Long loginUserId) {
        return new ReviewResponse(
                review.getId(),
                AuthorInfo.from(author),
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
