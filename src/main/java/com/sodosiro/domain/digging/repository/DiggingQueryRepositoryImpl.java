package com.sodosiro.domain.digging.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.digging.entity.Digging;
import com.sodosiro.domain.digging.entity.QDigging;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DiggingQueryRepositoryImpl implements DiggingQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QDigging d = QDigging.digging;

    @Override
    public List<Digging> findFeed(Long cursor, int size) {
        return baseQuery(cursor, size).fetch();
    }

    @Override
    public List<Digging> findByContentId(Long contentId, Long cursor, int size) {
        return baseQuery(cursor, size)
                .where(d.contentId.eq(contentId))
                .orderBy(d.createdAt.desc())
                .fetch();
    }

    @Override
    public List<Digging> findByUserId(Long userId, Long cursor, int size) {
        return baseQuery(cursor, size)
                .where(d.userId.eq(userId))
                .fetch();
    }

    private com.querydsl.jpa.impl.JPAQuery<Digging> baseQuery(Long cursor, int size) {
        return queryFactory
                .selectFrom(d)
                .where(
                        d.isDeleted.isFalse(),
                        d.id.lt(cursor)
                )
                .orderBy(d.id.desc())
                .limit(size);
    }
}
