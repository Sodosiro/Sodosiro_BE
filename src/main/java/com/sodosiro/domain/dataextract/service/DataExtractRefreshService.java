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

    public int accept(String runId, List<Long> contentIds) {
        validateRequest(runId, contentIds);

        List<Long> distinctIds = List.copyOf(new LinkedHashSet<>(contentIds));
        dataExtractEmbeddingProcessor.process(runId, distinctIds);
        return distinctIds.size();
    }

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
