package com.sodosiro.domain.dataextract.application.port.out;

/** ETL 임베딩 처리 상태 접근 경계. */
public interface EmbeddingStatePort {

    /**
 * Retrieves the embedding processing overview for the specified content.
 *
 * @param contentId the identifier of the content
 * @return the embedding processing overview
 */
String findOverview(Long contentId);

    /**
 * Marks embedding processing as completed for the specified content.
 */
void completeEmbedding(Long contentId);
}
