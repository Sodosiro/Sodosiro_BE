package com.sodosiro.domain.travel.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public interface SpotEmbeddingQueryRepository {

    /**
     * sigunguCode로 하드 필터링(WHERE)한 뒤, 코사인 거리(embedding <=> queryVector) 기준 유사도 검색한다.
     * categoryCodes가 비어있지 않으면 해당 카테고리를 제외하는 게 아니라 우선순위로 앞쪽에 배치한다(가중치).
     */
    List<Long> findNearestContentIds(float[] queryEmbedding, List<Integer> categoryCodes, String sigunguCode, int limit);

    /**
     * requiredCategoryCodes와 sigunguCode로 하드 필터링(WHERE)한 뒤, 그 안에서 코사인 거리 기준 유사도 검색한다.
     * boostCategoryCodes가 비어있지 않으면 필터링된 결과 안에서 해당 카테고리를 우선순위로 앞쪽에 배치한다(가중치).
     */
    List<Long> findNearestContentIdsInCategories(float[] queryEmbedding, List<Integer> requiredCategoryCodes, List<Integer> boostCategoryCodes, String sigunguCode, int limit);
    List<RelatedEmbeddingCandidate> findRelatedCandidates(Long contentId, int limit);

    /**
     * category가 같고 (centerLat, centerLon)으로부터 radiusKm 이내이며 excludeContentIds에 없는 후보를,
     * 코사인 거리(embedding <=> queryVector) 기준 유사도순으로 상위 limit개 반환한다.
     * 반경을 넓혀가며 여러 번 호출할 때 이미 채택한 후보(더 가까운 반경에서 찾은 것)를 다시 뽑지 않도록
     * 호출부가 excludeContentIds를 누적해서 넘긴다.
     */
    List<RelatedEmbeddingCandidate> findAlternativeCandidates(
            Integer category, BigDecimal centerLat, BigDecimal centerLon, float[] queryEmbedding,
            double radiusKm, int limit, Set<Long> excludeContentIds);

    record RelatedEmbeddingCandidate(Long contentId, Double distance) { }
}
