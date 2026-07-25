package com.sodosiro.domain.user.event;

import com.sodosiro.global.s3.service.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProfileImageEventListener {

    private final S3Service s3Service;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommit(ProfileImageChangedEvent event) {
        String oldUrl = event.oldUrl();
        if (oldUrl != null && !oldUrl.isBlank()) {
            try {
                s3Service.delete(oldUrl);
            } catch (Exception e) {
                log.warn("oldUrl 삭제 실패, 고아 파일로 남음: {}", oldUrl, e);
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_ROLLBACK)
    public void onRollback(ProfileImageChangedEvent event) {
        try {
            s3Service.delete(event.newUrl());
        } catch (Exception e) {
            log.warn("롤백 시 newUrl 삭제 실패, 고아 파일로 남음: {}", event.newUrl(), e);
        }
    }
}