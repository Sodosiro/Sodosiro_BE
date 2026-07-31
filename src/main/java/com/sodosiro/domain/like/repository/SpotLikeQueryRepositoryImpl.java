package com.sodosiro.domain.like.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.like.entity.QSpotLike;
import com.sodosiro.domain.like.entity.SpotLike;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class SpotLikeQueryRepositoryImpl implements SpotLikeQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QSpotLike l = QSpotLike.spotLike;

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
}
