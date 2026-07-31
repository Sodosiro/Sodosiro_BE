package com.sodosiro.domain.review.controller.dto.response;

import com.sodosiro.domain.review.entity.Review;
import com.sodosiro.domain.review.entity.ReviewImage;
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
        boolean isMyReview
) {
    public record AuthorInfo(Long userId, String displayName, String profileImageUrl) {
        /**
         * Creates author information from a user.
         *
         * @param user the user whose author information is created
         * @return author information containing the user's ID, display name, and profile image URL
         */
        public static AuthorInfo from(User user) {
            return new AuthorInfo(user.getUserId(), user.getDisplayName(), user.getProfileImageUrl());
        }
    }

    /**
     * Creates a review response containing review details, author information, images, and ownership status.
     *
     * @param review       the review to represent
     * @param author       the review author
     * @param images       the images associated with the review
     * @param loginUserId  the ID of the logged-in user
     * @return             the review response
     */
    public static ReviewResponse of(Review review, User author, List<ReviewImage> images, Long loginUserId) {
        return new ReviewResponse(
                review.getId(),
                AuthorInfo.from(author),
                review.getRating(),
                review.getBody(),
                images.stream().map(ReviewImageResponse::from).toList(),
                review.getCreatedAt(),
                review.getUserId().equals(loginUserId)
        );
    }
}
