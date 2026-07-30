package com.sodosiro.domain.review.repository;

import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.review.constants.ReviewSort;
import com.sodosiro.domain.review.entity.QReview;
import com.sodosiro.domain.review.entity.Review;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class ReviewQueryRepositoryImpl implements ReviewQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QReview r = QReview.review;

    @Override
    public List<Review> findByContentId(Long contentId, Long cursor, int size, ReviewSort sort) {
        return queryFactory
                .selectFrom(r)
                .where(
                        r.contentId.eq(contentId),
                        r.isDeleted.isFalse(),
                        r.id.lt(cursor)
                )
                .orderBy(orderBy(sort))
                .limit(size)
                .fetch();
    }

    @Override
    public List<Review> findByUserId(Long userId, Long cursor, int size) {
        return queryFactory
                .selectFrom(r)
                .where(
                        r.userId.eq(userId),
                        r.isDeleted.isFalse(),
                        r.id.lt(cursor)
                )
                .orderBy(r.createdAt.desc(), r.id.desc())
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

    private OrderSpecifier<?>[] orderBy(ReviewSort sort) {
        return switch (sort) {
            case HIGH_RATING -> new OrderSpecifier[]{r.rating.desc(), r.createdAt.desc()};
            case LOW_RATING  -> new OrderSpecifier[]{r.rating.asc(),  r.createdAt.desc()};
            default          -> new OrderSpecifier[]{r.createdAt.desc(), r.id.desc()};
        };
    }
}
