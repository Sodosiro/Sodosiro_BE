package com.sodosiro.domain.dataextract.service;

import com.sodosiro.domain.dataextract.application.port.out.EmbeddingStatePort;
import com.sodosiro.domain.dataextract.application.port.out.TravelEmbeddingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

/** ETL이 전달한 관광지의 임베딩 생성과 완료 상태 갱신을 담당한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataExtractEmbeddingProcessor {

    private static final int EMBEDDING_DIMENSIONS = 1536;

    private static final int MAX_CONCURRENCY = 6;

    private final TravelEmbeddingPort travelEmbeddingPort;
    private final EmbeddingStatePort embeddingStatePort;
    private final TravelKeywordExtractor travelKeywordExtractor;
    private final EmbeddingModel embeddingModel;
    private final TransactionTemplate transactionTemplate;

    @Async
    public void process(String runId, List<Long> contentIds) {
        Semaphore permits = new Semaphore(MAX_CONCURRENCY);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (Long contentId : contentIds) {
                executor.submit(() -> {
                    try {
                        permits.acquire();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        log.error("ETL run {}: 관광지 {} 임베딩 처리 중단(인터럽트)", runId, contentId);
                        return;
                    }
                    try {
                        transactionTemplate.executeWithoutResult(status -> processOne(contentId));
                    } catch (RuntimeException exception) {
                        log.error("ETL run {}: 관광지 {} 임베딩 처리 실패", runId, contentId, exception);
                    } finally {
                        permits.release();
                    }
                });
            }
        }
    }

    private void processOne(Long contentId) {
        String title = travelEmbeddingPort.findSpotTitle(contentId);
        String overview = embeddingStatePort.findOverview(contentId);
        TravelKeywordExtractor.Extraction extraction = travelKeywordExtractor.extract(title, overview);
        String embeddingText = extraction.embeddingText(title);
        float[] embedding = embeddingModel.embed(embeddingText);
        if (embedding.length != EMBEDDING_DIMENSIONS) {
            throw new IllegalStateException("예상하지 못한 임베딩 차원입니다: "
                    + embedding.length);
        }

        travelEmbeddingPort.saveEmbedding(contentId, embedding, embeddingText, extraction.keywordText());
        embeddingStatePort.completeEmbedding(contentId);
    }

}
