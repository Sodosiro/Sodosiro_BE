package com.sodosiro.domain.travel.repository;

import java.util.List;

public interface SpotEmbeddingQueryRepository {
    List<RelatedEmbeddingCandidate> findRelatedCandidates(Long contentId, int limit);

    record RelatedEmbeddingCandidate(Long contentId, Double distance) { }
}
