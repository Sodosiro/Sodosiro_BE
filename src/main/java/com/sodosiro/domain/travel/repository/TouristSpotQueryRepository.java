package com.sodosiro.domain.travel.repository;

import java.math.BigDecimal;

public interface TouristSpotQueryRepository {

    /**
 * Updates the average rating and review count for a tourist spot.
 *
 * @param contentId   the tourist spot's content identifier
 * @param avgRating   the updated average rating
 * @param reviewCount the updated number of reviews
 */
void updateRatingStats(Long contentId, BigDecimal avgRating, Integer reviewCount);
}
