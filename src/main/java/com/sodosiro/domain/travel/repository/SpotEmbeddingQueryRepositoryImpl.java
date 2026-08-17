package com.sodosiro.domain.travel.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** Querydsl은 pgvector 연산자(<=>)를 지원하지 않아 네이티브 쿼리로 직접 처리한다. */
@Repository
@RequiredArgsConstructor
public class SpotEmbeddingQueryRepositoryImpl implements SpotEmbeddingQueryRepository {

    private final EntityManager entityManager;

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
        sql.append(" WHERE s.sigungu_code = :sigunguCode");
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
        sql.append(" WHERE s.category IN (:requiredCategoryCodes) AND s.sigungu_code = :sigunguCode");
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
