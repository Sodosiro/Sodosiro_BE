package com.sodosiro.domain.travel.repository;

import java.util.List;

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
}
