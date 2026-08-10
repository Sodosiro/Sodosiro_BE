package com.sodosiro.domain.travel.repository;

import java.util.List;

public interface SpotEmbeddingQueryRepository {

    /**
     * 코사인 거리(embedding <=> queryVector) 기준 유사도 검색. 전체 카테고리를 대상으로 검색하되,
     * categoryCodes가 비어있지 않으면 해당 카테고리를 제외하는 게 아니라 우선순위로 앞쪽에 배치한다(가중치).
     */
    List<Long> findNearestContentIds(float[] queryEmbedding, List<Integer> categoryCodes, int limit);
}
