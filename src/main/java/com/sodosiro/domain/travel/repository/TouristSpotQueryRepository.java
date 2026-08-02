package com.sodosiro.domain.travel.repository;

import java.math.BigDecimal;

public interface TouristSpotQueryRepository {

    void updateRatingStats(Long contentId, BigDecimal avgRating, Integer reviewCount);

    void incrementLikeCount(Long contentId);

    void decrementLikeCount(Long contentId);
}
