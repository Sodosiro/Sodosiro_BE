package com.sodosiro.domain.review.repository;

import com.sodosiro.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewQueryRepository {

    /**
 * Determines whether a non-deleted review exists for the specified content and user.
 *
 * @param contentId the content identifier
 * @param userId    the user identifier
 * @return {@code true} if a non-deleted review exists, {@code false} otherwise
 */
boolean existsByContentIdAndUserIdAndIsDeletedFalse(Long contentId, Long userId);

    /**
 * Finds a review by its identifier when it has not been deleted.
 *
 * @param id the review identifier
 * @return the matching review, or an empty optional if no non-deleted review exists
 */
Optional<Review> findByIdAndIsDeletedFalse(Long id);
}
