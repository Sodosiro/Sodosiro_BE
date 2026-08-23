package com.sodosiro.domain.digging.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.digging.entity.QDiggingImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class DiggingImageQueryRepositoryImpl implements DiggingImageQueryRepository {

    private static final QDiggingImage diggingImage = QDiggingImage.diggingImage;

    private final JPAQueryFactory queryFactory;

    @Override
    public void deleteAllByDiggingId(Long diggingId) {
        queryFactory
                .delete(diggingImage)
                .where(diggingImage.diggingId.eq(diggingId))
                .execute();
    }
}
