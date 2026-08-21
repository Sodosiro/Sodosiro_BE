package com.sodosiro.domain.review.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.review.constants.ReviewSort;
import com.sodosiro.domain.review.entity.QReview;
import com.sodosiro.domain.review.entity.QReviewImage;
import com.sodosiro.domain.review.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class ReviewQueryRepositoryImpl implements ReviewQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QReview r = QReview.review;
    private static final QReviewImage ri = QReviewImage.reviewImage;

    @Override
    public List<Review> findByContentId(Long contentId, Long cursor, int size, ReviewSort sort, boolean hasImage) {
        return queryFactory
                .selectFrom(r)
                .where(
                        r.contentId.eq(contentId),
                        r.isDeleted.isFalse(),
                        r.id.lt(cursor),
                        hasImageFilter(hasImage)
                )
                .orderBy(orderBy(sort))
                .limit(size)
                .fetch();
    }

    private BooleanExpression hasImageFilter(boolean hasImage) {
        if (!hasImage) return null;
        return JPAExpressions.selectOne()
                .from(ri)
                .where(ri.reviewId.eq(r.id))
                .exists();
    }

    @Override
    public List<Review> findByUserId(Long userId, Long cursor, int size, ReviewSort sort, boolean hasImage) {
        return queryFactory
                .selectFrom(r)
                .where(
                        r.userId.eq(userId),
                        r.isDeleted.isFalse(),
                        r.id.lt(cursor),
                        hasImageFilter(hasImage)
                )
                .orderBy(orderBy(sort))
                .limit(size)
                .fetch();
    }

    @Override
    public Double avgRatingByContentId(Long contentId) {
        return queryFactory
                .select(r.rating.avg())
                .from(r)
                .where(
                        r.contentId.eq(contentId),
                        r.isDeleted.isFalse()
                )
                .fetchOne();
    }

    @Override
    public long countActiveByContentId(Long contentId) {
        Long count = queryFactory
                .select(r.count())
                .from(r)
                .where(
                        r.contentId.eq(contentId),
                        r.isDeleted.isFalse()
                )
                .fetchOne();
        return count == null ? 0L : count;
    }

    @Override
    public Set<ReviewKey> findWrittenKeys(Collection<Long> userIds, Collection<Long> contentIds) {
        if (userIds.isEmpty() || contentIds.isEmpty()) {
            return Set.of();
        }
        return queryFactory
                .select(Projections.constructor(ReviewKey.class, r.userId, r.contentId))
                .from(r)
                .where(
                        r.userId.in(userIds),
                        r.contentId.in(contentIds),
                        r.isDeleted.isFalse()
                )
                .fetch()
                .stream()
                .collect(Collectors.toUnmodifiableSet());
    }

    private OrderSpecifier<?>[] orderBy(ReviewSort sort) {
        return switch (sort) {
            case HIGH_RATING -> new OrderSpecifier[]{r.rating.desc(), r.createdAt.desc()};
            case LOW_RATING  -> new OrderSpecifier[]{r.rating.asc(),  r.createdAt.desc()};
            default          -> new OrderSpecifier[]{r.createdAt.desc(), r.id.desc()};
        };
    }
}
