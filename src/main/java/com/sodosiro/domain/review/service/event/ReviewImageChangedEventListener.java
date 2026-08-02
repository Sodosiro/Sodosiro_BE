package com.sodosiro.domain.review.service.event;

import com.sodosiro.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewImageChangedEventListener {

    private final S3Service s3Service;

    @Async("reviewImageExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommit(ReviewImageChangedEvent event) {
        deleteQuietly(event.oldUrls(), "커밋 후 구 이미지 삭제");
    }

    @Async("reviewImageExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onRollback(ReviewImageChangedEvent event) {
        deleteQuietly(event.newUrls(), "롤백 후 고아 이미지 삭제");
    }

    private void deleteQuietly(java.util.List<String> urls, String reason) {
        if (urls == null || urls.isEmpty()) return;
        try {
            s3Service.delete(urls);
        } catch (Exception e) {
            log.warn("[ReviewImage] {} 실패, 고아 파일 남음: {}", reason, urls, e);
        }
    }
}
