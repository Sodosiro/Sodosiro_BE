package com.sodosiro.domain.user.service.event;

import com.sodosiro.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProfileImageEventListener {

    private final S3Service s3Service;

    @Async("profileImageExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommit(ProfileImageChangedEvent event) {
        String oldUrl = event.oldUrl();

        if (oldUrl != null && !oldUrl.isBlank()) {
            deleteS3Image(oldUrl);
        }
    }

    @Async("profileImageExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onRollback(ProfileImageChangedEvent event) {
        String newUrl = event.newUrl();

        if (newUrl != null && !newUrl.isBlank()) {
            deleteS3Image(newUrl);
        }
    }

    private void deleteS3Image(String imageUrl) {
        try {
            s3Service.delete(imageUrl);
        } catch (Exception e) {
            log.warn("S3 이미지 삭제 실패, 고아 파일로 남음: {}", imageUrl, e);
            // TODO: 리뷰어가 요구한 실패 시 재시도 정책이나 추가 예외 처리를 추후 작성
        }
    }

}