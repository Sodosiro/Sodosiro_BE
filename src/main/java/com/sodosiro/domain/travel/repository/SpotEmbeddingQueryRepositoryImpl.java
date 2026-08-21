package com.sodosiro.domain.travel.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.travel.entity.QSpotEmbedding;
import com.sodosiro.domain.travel.entity.QTouristSpot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * sigunguCode 하드 필터(WHERE) + 코사인 거리 기준 유사도 검색(findNearestContentIds*)은 QueryDSL이
 * pgvector 연산자(<=>)를 지원하지 않아 네이티브 쿼리로 직접 처리한다.
 * 관련 여행지 추천(findRelatedCandidates)은 Hibernate Vector의 cosine_distance 함수를 QueryDSL에서
 * 호출한다 — PostgreSQL 방언에서는 이 함수가 pgvector의 {@code <=>} 연산자로 렌더링된다.
 */
@Repository
@RequiredArgsConstructor
public class SpotEmbeddingQueryRepositoryImpl implements SpotEmbeddingQueryRepository {

    private static final QSpotEmbedding source = new QSpotEmbedding("sourceEmbedding");
    private static final QSpotEmbedding candidate = new QSpotEmbedding("candidateEmbedding");
    private static final QTouristSpot candidateSpot = new QTouristSpot("candidateSpot");

    private final EntityManager entityManager;
    private final JPAQueryFactory queryFactory;

    /**
     * sigunguCode는 하드 필터(WHERE)다. categoryCodes는 후보를 제외하는 필터가 아니라 우선순위 가중치다.
     * 선택한 카테고리에 속한 관광지를 먼저(유사도순), 그 다음 나머지 전체 카테고리를 유사도순으로 붙인다.
     * 그래서 지역 안 전체 카테고리를 대상으로 검색하되 선택한 카테고리가 더 많이/앞쪽에 추천된다.
     */
    @Override
    public List<Long> findNearestContentIds(
            float[] queryEmbedding, List<Integer> categoryCodes, String sigunguCode, int limit) {
        boolean hasCategoryBoost = categoryCodes != null && !categoryCodes.isEmpty();

        StringBuilder sql = new StringBuilder(
                "SELECT e.content_id FROM spot_embedding e JOIN tourist_spot s ON s.content_id = e.content_id");
        sql.append(" WHERE s.ldong_signgu_code = :sigunguCode");
        sql.append(" ORDER BY ");
        if (hasCategoryBoost) {
            sql.append("(CASE WHEN s.category IN (:categoryCodes) THEN 0 ELSE 1 END), ");
        }
        sql.append("e.embedding <=> CAST(:queryVector AS vector) LIMIT :limit");

        Query query = entityManager.createNativeQuery(sql.toString())
                .setParameter("queryVector", toVectorLiteral(queryEmbedding))
                .setParameter("sigunguCode", sigunguCode)
                .setParameter("limit", limit);
        if (hasCategoryBoost) {
            query.setParameter("categoryCodes", categoryCodes);
        }

        return query.getResultList().stream()
                .map(row -> ((Number) row).longValue())
                .toList();
    }

    @Override
    public List<Long> findNearestContentIdsInCategories(
            float[] queryEmbedding, List<Integer> requiredCategoryCodes, List<Integer> boostCategoryCodes,
            String sigunguCode, int limit) {
        boolean hasBoost = boostCategoryCodes != null && !boostCategoryCodes.isEmpty();

        StringBuilder sql = new StringBuilder(
                "SELECT e.content_id FROM spot_embedding e JOIN tourist_spot s ON s.content_id = e.content_id");
        sql.append(" WHERE s.category IN (:requiredCategoryCodes) AND s.ldong_signgu_code = :sigunguCode");
        sql.append(" ORDER BY ");
        if (hasBoost) {
            sql.append("(CASE WHEN s.category IN (:boostCategoryCodes) THEN 0 ELSE 1 END), ");
        }
        sql.append("e.embedding <=> CAST(:queryVector AS vector) LIMIT :limit");

        Query query = entityManager.createNativeQuery(sql.toString())
                .setParameter("queryVector", toVectorLiteral(queryEmbedding))
                .setParameter("requiredCategoryCodes", requiredCategoryCodes)
                .setParameter("sigunguCode", sigunguCode)
                .setParameter("limit", limit);
        if (hasBoost) {
            query.setParameter("boostCategoryCodes", boostCategoryCodes);
        }

        return query.getResultList().stream()
                .map(row -> ((Number) row).longValue())
                .toList();
    }

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

    /**
     * 카테고리 + 반경(haversine, km) + 제외 목록 하드 필터(WHERE)를 SQL에서 직접 계산한 뒤,
     * 그 안에서 코사인 거리(embedding <=> queryVector) 기준 유사도순으로 상위 limit개를 뽑는다.
     * 좌표는 정확한 대권거리(great-circle distance) 공식을 SQL로 그대로 옮긴 것이라 bbox 근사보다 정확하다.
     * excludeContentIds는 항상 대상 장소 자신을 포함해 호출부에서 넘긴다(빈 컬렉션으로 호출하지 않는다).
     */
    @Override
    public List<RelatedEmbeddingCandidate> findAlternativeCandidates(
            Integer category, BigDecimal centerLat, BigDecimal centerLon, float[] queryEmbedding,
            double radiusKm, int limit, Set<Long> excludeContentIds) {

        String sql = "SELECT e.content_id, e.embedding <=> CAST(:queryVector AS vector) AS distance "
                + "FROM spot_embedding e JOIN tourist_spot s ON s.content_id = e.content_id "
                + "WHERE s.category = :category AND s.content_id NOT IN (:excludeContentIds) "
                + "AND s.map_x IS NOT NULL AND s.map_y IS NOT NULL AND e.embedding IS NOT NULL "
                + "AND (6371 * acos(LEAST(1, GREATEST(-1, "
                + "cos(radians(:centerLat)) * cos(radians(s.map_y)) * cos(radians(s.map_x) - radians(:centerLon)) "
                + "+ sin(radians(:centerLat)) * sin(radians(s.map_y)))))) <= :radiusKm "
                + "ORDER BY e.embedding <=> CAST(:queryVector AS vector) LIMIT :limit";

        List<?> rows = entityManager.createNativeQuery(sql)
                .setParameter("queryVector", toVectorLiteral(queryEmbedding))
                .setParameter("category", category)
                .setParameter("excludeContentIds", excludeContentIds)
                .setParameter("centerLat", centerLat)
                .setParameter("centerLon", centerLon)
                .setParameter("radiusKm", radiusKm)
                .setParameter("limit", limit)
                .getResultList();

        return rows.stream()
                .map(row -> {
                    Object[] columns = (Object[]) row;
                    return new RelatedEmbeddingCandidate(
                            ((Number) columns[0]).longValue(), ((Number) columns[1]).doubleValue());
                })
                .toList();
    }

    //float[] 배열(임베딩 벡터)을 PostgreSQL의 pgvector가 인식할 수 있는 텍스트 포맷으로 변환
    private static String toVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder(embedding.length * 8).append('[');
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(embedding[i]);
        }
        return builder.append(']').toString();
    }
}
