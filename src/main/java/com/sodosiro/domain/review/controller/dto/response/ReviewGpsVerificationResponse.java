package com.sodosiro.domain.review.controller.dto.response;

import com.sodosiro.domain.review.constants.ReviewVisitType;
import com.sodosiro.domain.review.entity.Review;
import java.time.LocalDateTime;

public record ReviewGpsVerificationResponse(
        Long reviewId,
        ReviewVisitType visitType,
        boolean gpsVerified,
        LocalDateTime gpsVerifiedAt
) {
    public static ReviewGpsVerificationResponse from(Review review) {
        boolean verified = review.getVisitType() == ReviewVisitType.GPS_VERIFIED;
        return new ReviewGpsVerificationResponse(
                review.getId(), review.getVisitType(), verified, review.getGpsVerifiedAt());
    }
}
