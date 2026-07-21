package com.sodosiro.domain.dataextract.application.port.out;

/** 여행지 임베딩 처리에 필요한 travel 도메인 접근 경계. */
public interface TravelEmbeddingPort {

    /**
 * Retrieves the title of the travel spot identified by the content ID.
 *
 * @param contentId the identifier of the travel spot
 * @return the travel spot title
 */
String findSpotTitle(Long contentId);

    /**
 * Persists an embedding and its source and category metadata for a travel spot.
 *
 * @param contentId   the travel spot identifier
 * @param embedding   the embedding vector
 * @param inputText   the source text containing the place name, classification, description, and keyword index
 * @param keywordText the category-filter keyword list, with the first token representing the primary category
 */
    void saveEmbedding(Long contentId, float[] embedding, String inputText, String keywordText);
}
