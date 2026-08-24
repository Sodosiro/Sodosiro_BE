package com.sodosiro.domain.like.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.like.entity.QSpotLike;
import com.sodosiro.domain.like.entity.SpotLike;
import com.sodosiro.domain.review.constants.ReviewSort;
import com.sodosiro.domain.travel.entity.QTouristSpot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpotLikeQueryRepositoryImpl implements SpotLikeQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QSpotLike l = QSpotLike.spotLike;
    private static final QTouristSpot spot = QTouristSpot.touristSpot;

    @Override
    public List<SpotLike> findByUserId(Long userId, Long cursor, int size) {
        return queryFactory
                .selectFrom(l)
                .where(
                        l.userId.eq(userId),
                        l.id.lt(cursor)
                )
                .orderBy(l.id.desc())
                .limit(size)
                .fetch();
    }

    @Override
    public List<SpotLike> findByUserIdAndFilters(
            Long userId, String sigunguCode, List<Integer> categories,
            Long likeIdCursor, BigDecimal ratingCursor, ReviewSort sort, int size) {
        return queryFactory
                .selectFrom(l)
                .join(l.touristSpot, spot).fetchJoin()
                .where(
                        l.userId.eq(userId),
                        sigunguCodeEq(sigunguCode),
                        categoryIn(categories),
                        cursorCondition(likeIdCursor, ratingCursor, sort)
                )
                .orderBy(orderBy(sort))
                .limit(size)
                .fetch();
    }

    @Override
    public long countByUserIdAndFilters(Long userId, String sigunguCode, List<Integer> categories) {
        Long count = queryFactory
                .select(l.count())
                .from(l)
                .join(l.touristSpot, spot)
                .where(
                        l.userId.eq(userId),
                        sigunguCodeEq(sigunguCode),
                        categoryIn(categories)
                )
                .fetchOne();
        return count != null ? count : 0L;
    }

    private BooleanExpression cursorCondition(Long likeIdCursor, BigDecimal ratingCursor, ReviewSort sort) {
        if (sort == ReviewSort.RECENT) {
            return l.id.lt(likeIdCursor);
        }
        if (ratingCursor == null || likeIdCursor == null) {
            return null;
        }
        BooleanExpression sameRatingWithOlderLike = spot.avgRating.eq(ratingCursor).and(l.id.lt(likeIdCursor));
        return sort == ReviewSort.HIGH_RATING
                ? spot.avgRating.lt(ratingCursor).or(sameRatingWithOlderLike)
                : spot.avgRating.gt(ratingCursor).or(sameRatingWithOlderLike);
    }

    private OrderSpecifier<?>[] orderBy(ReviewSort sort) {
        return switch (sort) {
            case HIGH_RATING -> new OrderSpecifier[]{spot.avgRating.desc(), l.id.desc()};
            case LOW_RATING -> new OrderSpecifier[]{spot.avgRating.asc(), l.id.desc()};
            case RECENT -> new OrderSpecifier[]{l.id.desc()};
        };
    }

    private BooleanExpression sigunguCodeEq(String sigunguCode) {
        return sigunguCode != null ? spot.sigunguCode.eq(sigunguCode) : null;
    }

    private BooleanExpression categoryIn(List<Integer> categories) {
        return (categories != null && !categories.isEmpty()) ? spot.category.in(categories) : null;
    }
}
