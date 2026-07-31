package com.sodosiro.domain.travel.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.travel.entity.QKakaoSpot;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class KakaoSpotQueryRepositoryImpl implements KakaoSpotQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QKakaoSpot kakaoSpot = QKakaoSpot.kakaoSpot;

    @Override
    public void incrementLikeCount(Long kakaoSpotId) {
        queryFactory
                .update(kakaoSpot)
                .set(kakaoSpot.likeCount, kakaoSpot.likeCount.add(1))
                .where(kakaoSpot.id.eq(kakaoSpotId))
                .execute();
    }

    @Override
    public void decrementLikeCount(Long kakaoSpotId) {
        queryFactory
                .update(kakaoSpot)
                .set(kakaoSpot.likeCount, kakaoSpot.likeCount.subtract(1))
                .where(kakaoSpot.id.eq(kakaoSpotId), kakaoSpot.likeCount.gt(0))
                .execute();
    }
}
