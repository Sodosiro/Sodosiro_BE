package com.sodosiro.domain.dataextract.service;

import com.sodosiro.domain.dataextract.application.port.out.EmbeddingStatePort;
import com.sodosiro.domain.dataextract.application.port.out.TravelEmbeddingPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
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

    /** 최초 실행 실패 뒤 최대 두 번 재시도한다 (1초, 2초 지수 백오프). */
    private static final int MAX_RETRY_COUNT = 2;
    private static final long INITIAL_BACKOFF_MILLIS = 1_000L;

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
                        processWithRetry(runId, contentId);
                    } finally {
                        permits.release();
                    }
                });
            }
        }
    }

    private void processWithRetry(String runId, Long contentId) {
        for (int retryCount = 0; ; retryCount++) {
            try {
                transactionTemplate.executeWithoutResult(status -> processOne(contentId));
                return;
            } catch (RuntimeException exception) {
                if (!isRetryable(exception) || retryCount >= MAX_RETRY_COUNT) {
                    log.error("ETL run {}: 관광지 {} 임베딩 처리 실패 (시도 {}회)",
                            runId, contentId, retryCount + 1, exception);
                    return;
                }

                long backoffMillis = INITIAL_BACKOFF_MILLIS * (1L << retryCount);
                log.warn("ETL run {}: 관광지 {} 임베딩 처리 일시 실패. {}ms 후 재시도 ({}/{})",
                        runId, contentId, backoffMillis, retryCount + 1, MAX_RETRY_COUNT);
                if (!sleepBeforeRetry(runId, contentId, backoffMillis)) {
                    return;
                }
            }
        }
    }

    private static boolean sleepBeforeRetry(String runId, Long contentId, long backoffMillis) {
        try {
            Thread.sleep(backoffMillis);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.error("ETL run {}: 관광지 {} 임베딩 재시도 대기 중단", runId, contentId);
            return false;
        }
    }

    /** 연결 오류, rate limit(429), 서버 오류(5xx)만 일시 오류로 취급한다. */
    static boolean isRetryable(Throwable exception) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ResourceAccessException) {
                return true;
            }
            if (cause instanceof RestClientResponseException responseException
                    && (responseException.getStatusCode().value() == 429
                    || responseException.getStatusCode().is5xxServerError())) {
                return true;
            }
        }
        return false;
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
