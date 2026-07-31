package com.sodosiro.domain.travel.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.travel.entity.QTouristSpot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
@RequiredArgsConstructor
public class TouristSpotQueryRepositoryImpl implements TouristSpotQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QTouristSpot spot = QTouristSpot.touristSpot;

    @Override
    public void updateRatingStats(Long contentId, BigDecimal avgRating, Integer reviewCount) {
        queryFactory
                .update(spot)
                .set(spot.avgRating, avgRating)
                .set(spot.reviewCount, reviewCount)
                .where(spot.contentId.eq(contentId))
                .execute();
    }
}
