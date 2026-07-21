package com.sodosiro.domain.dataextract.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DataExtractRefreshService {

    private static final int MAX_BATCH_SIZE = 500;

    private final DataExtractEmbeddingProcessor dataExtractEmbeddingProcessor;

    /**
     * Processes a batch of distinct content identifiers for a run.
     *
     * @param runId      the identifier of the run
     * @param contentIds the content identifiers to process
     * @return the number of distinct content identifiers processed
     */
    public int accept(String runId, List<Long> contentIds) {
        validateRequest(runId, contentIds);

        List<Long> distinctIds = List.copyOf(new LinkedHashSet<>(contentIds));
        dataExtractEmbeddingProcessor.process(runId, distinctIds);
        return distinctIds.size();
    }

    /**
     * Validates the run identifier and content ID batch.
     *
     * @param runId      the identifier of the processing run
     * @param contentIds the content IDs to validate
     * @throws IllegalArgumentException if the run ID is blank, the content ID list is empty,
     *                                  exceeds the maximum batch size, or contains a non-positive ID
     */
    private static void validateRequest(String runId, List<Long> contentIds) {
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId는 필수입니다.");
        }
        if (contentIds == null || contentIds.isEmpty()) {
            throw new IllegalArgumentException("contentIds는 하나 이상 필요합니다.");
        }
        if (contentIds.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException("contentIds는 최대 " + MAX_BATCH_SIZE + "건입니다.");
        }
        if (contentIds.stream().anyMatch(id -> id == null || id <= 0)) {
            throw new IllegalArgumentException("contentIds에는 양수만 넣을 수 있습니다.");
        }
    }
}
