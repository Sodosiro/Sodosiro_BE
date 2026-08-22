package com.sodosiro.domain.review.repository;

import com.sodosiro.domain.review.constants.ReviewSort;
import com.sodosiro.domain.review.entity.Review;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ReviewQueryRepository {

    List<Review> findByContentId(Long contentId, Long cursor, int size, ReviewSort sort, boolean hasImage);

    List<Review> findByUserId(Long userId, Long cursor, int size, ReviewSort sort, boolean hasImage);

    Double avgRatingByContentId(Long contentId);

    long countActiveByContentId(Long contentId);

    Set<ReviewKey> findWrittenKeys(Collection<Long> userIds, Collection<Long> contentIds);
}
