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

    /**
     * Retrieves active reviews for a content item before the specified cursor.
     *
     * @param contentId the content identifier
     * @param cursor    the exclusive upper bound for review IDs
     * @param size      the maximum number of reviews to retrieve
     * @param sort      the ordering applied to the reviews
     * @return the matching reviews
     */
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

    /**
     * Retrieves active reviews written by a user before the specified cursor.
     *
     * @param userId the ID of the user whose reviews are retrieved
     * @param cursor the exclusive upper-bound review ID
     * @param size the maximum number of reviews to return
     * @return reviews ordered from newest to oldest
     */
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

    /**
     * Calculates the average rating of active reviews for a content item.
     *
     * @param contentId the identifier of the content item
     * @return the average rating, or {@code null} when no active reviews exist
     */
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

    /**
     * Counts the active reviews associated with a content item.
     *
     * @param contentId the content item identifier
     * @return the number of active reviews, or {@code 0} when none exist
     */
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

    /**
     * Builds the review ordering criteria for the requested sort.
     *
     * @param sort the review sorting option
     * @return ordering criteria for the selected sort
     */
    private OrderSpecifier<?>[] orderBy(ReviewSort sort) {
        return switch (sort) {
            case HIGH_RATING -> new OrderSpecifier[]{r.rating.desc(), r.createdAt.desc()};
            case LOW_RATING  -> new OrderSpecifier[]{r.rating.asc(),  r.createdAt.desc()};
            default          -> new OrderSpecifier[]{r.createdAt.desc(), r.id.desc()};
        };
    }
}
