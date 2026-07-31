package com.sodosiro.domain.review.repository;

import com.sodosiro.domain.review.constants.ReviewSort;
import com.sodosiro.domain.review.entity.Review;

import java.util.List;

public interface ReviewQueryRepository {

    /**
 * Retrieves reviews for a content item using cursor-based pagination and the specified sort order.
 *
 * @param contentId the identifier of the content item
 * @param cursor    the pagination cursor
 * @param size      the maximum number of reviews to retrieve
 * @param sort      the review sort order
 * @return the matching reviews
 */
List<Review> findByContentId(Long contentId, Long cursor, int size, ReviewSort sort);

    /**
 * Retrieves reviews authored by a user using cursor-based pagination.
 *
 * @param userId the user's identifier
 * @param cursor the pagination cursor
 * @param size   the maximum number of reviews to retrieve
 * @return the user's reviews
 */
List<Review> findByUserId(Long userId, Long cursor, int size);

    /**
 * Calculates the average rating for reviews associated with a content item.
 *
 * @param contentId the identifier of the content item
 * @return the average review rating for the content item
 */
Double avgRatingByContentId(Long contentId);

    /**
 * Counts active reviews for a content item.
 *
 * @param contentId the identifier of the content item
 * @return the number of active reviews
 */
long countActiveByContentId(Long contentId);
}
