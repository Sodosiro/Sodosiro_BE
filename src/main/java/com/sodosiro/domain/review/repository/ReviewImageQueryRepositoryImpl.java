package com.sodosiro.domain.review.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.review.entity.QReviewImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReviewImageQueryRepositoryImpl implements ReviewImageQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QReviewImage ri = QReviewImage.reviewImage;

    @Override
    public void deleteAllByReviewId(Long reviewId) {
        queryFactory
                .delete(ri)
                .where(ri.reviewId.eq(reviewId))
                .execute();
    }
}
