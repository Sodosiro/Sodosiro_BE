package com.sodosiro.domain.dataextract.application.port.out;

/** ETL 임베딩 처리 상태 접근 경계. */
public interface EmbeddingStatePort {

    String findOverview(Long contentId);

    void completeEmbedding(Long contentId);
}
