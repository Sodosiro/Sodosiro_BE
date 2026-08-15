package com.sodosiro.domain.like.repository;

import com.sodosiro.domain.like.entity.SpotLike;
import com.sodosiro.domain.review.constants.ReviewSort;

import java.math.BigDecimal;
import java.util.List;

public interface SpotLikeQueryRepository {

    List<SpotLike> findByUserId(Long userId, Long cursor, int size);

    List<SpotLike> findByUserIdAndFilters(
            Long userId, String sigunguCode, Long likeIdCursor, BigDecimal ratingCursor, ReviewSort sort, int size);
}
