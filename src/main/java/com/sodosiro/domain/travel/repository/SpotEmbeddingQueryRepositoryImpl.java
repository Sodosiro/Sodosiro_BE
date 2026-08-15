package com.sodosiro.domain.travel.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.travel.entity.QSpotEmbedding;
import com.sodosiro.domain.travel.entity.QTouristSpot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * Hibernate Vector의 cosine_distance 함수를 QueryDSL에서 호출한다.
 * PostgreSQL 방언에서는 함수가 pgvector의 {@code <=>} 연산자로 렌더링된다.
 */
@Repository
@RequiredArgsConstructor
public class SpotEmbeddingQueryRepositoryImpl implements SpotEmbeddingQueryRepository {

    private static final QSpotEmbedding source = new QSpotEmbedding("sourceEmbedding");
    private static final QSpotEmbedding candidate = new QSpotEmbedding("candidateEmbedding");
    private static final QTouristSpot candidateSpot = new QTouristSpot("candidateSpot");

    private final JPAQueryFactory queryFactory;

    @Override
    public List<RelatedEmbeddingCandidate> findRelatedCandidates(Long contentId, int limit) {
        NumberExpression<Double> distance = Expressions.numberTemplate(Double.class,
                "cosine_distance({0}, {1})", candidate.embedding, source.embedding);

        return queryFactory.select(Projections.constructor(RelatedEmbeddingCandidate.class,
                        candidate.contentId, distance))
                .from(source)
                .join(candidate).on(candidate.contentId.ne(source.contentId))
                .join(candidateSpot).on(candidateSpot.contentId.eq(candidate.contentId))
                .where(source.contentId.eq(contentId), source.embedding.isNotNull(), candidate.embedding.isNotNull())
                .orderBy(distance.asc(), candidate.contentId.asc())
                .limit(limit)
                .fetch();
    }
}
