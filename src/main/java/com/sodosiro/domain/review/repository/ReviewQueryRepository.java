package com.sodosiro.domain.review.repository;

import com.sodosiro.domain.review.constants.ReviewSort;
import com.sodosiro.domain.review.entity.Review;

import java.util.List;

public interface ReviewQueryRepository {

    List<Review> findByContentId(Long contentId, Long cursor, int size, ReviewSort sort);

    List<Review> findByUserId(Long userId, Long cursor, int size);

    Double avgRatingByContentId(Long contentId);

    long countActiveByContentId(Long contentId);
}
