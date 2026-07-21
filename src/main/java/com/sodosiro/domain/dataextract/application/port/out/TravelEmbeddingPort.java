package com.sodosiro.domain.dataextract.application.port.out;

/** 여행지 임베딩 처리에 필요한 travel 도메인 접근 경계. */
public interface TravelEmbeddingPort {

    String findSpotTitle(Long contentId);

    /**
     * @param inputText   임베딩 원문 (장소명·분류·설명·키워드 색인문)
     * @param keywordText 카테고리 필터용 키워드 목록 — 첫 토큰이 대표 분류
     */
    void saveEmbedding(Long contentId, float[] embedding, String inputText, String keywordText);
}
