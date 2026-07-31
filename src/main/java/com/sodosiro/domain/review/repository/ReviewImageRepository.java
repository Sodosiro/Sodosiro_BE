package com.sodosiro.domain.review.repository;

import com.sodosiro.domain.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long> {

    /**
 * Retrieves all images associated with a review in display order.
 *
 * @param reviewId the identifier of the review
 * @return the review's images ordered by display order ascending
 */
List<ReviewImage> findAllByReviewIdOrderByDisplayOrderAsc(Long reviewId);

    /**
 * Finds all images associated with the specified reviews, ordered by review ID
 * and then display order in ascending order.
 *
 * @param reviewIds the review identifiers whose images are retrieved
 * @return the matching review images
 */
List<ReviewImage> findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc(List<Long> reviewIds);

    /**
 * Deletes all images associated with the specified review.
 *
 * @param reviewId the identifier of the review
 */
void deleteAllByReviewId(Long reviewId);
}
