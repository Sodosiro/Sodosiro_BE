package com.sodosiro.domain.user.service.event;

import com.sodosiro.domain.course.service.ActiveCourseCacheWriter;
import com.sodosiro.domain.jwt.JwtProvider;
import com.sodosiro.domain.like.service.dto.LikedSpotsGeoCache;
import com.sodosiro.global.service.RedisService;
import com.sodosiro.global.utils.TokenKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawEventListener {

    private static final Duration WITHDRAWN_MARKER_MARGIN = Duration.ofDays(1);

    private final RedisService redisService;
    private final ActiveCourseCacheWriter activeCourseCacheWriter;
    private final JwtProvider jwtProvider;

    @Value("${user.withdrawal.retention-days:7}")
    private int retentionDays;

    @Async("withdrawExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(WithdrawEvent event) {
        try {
            clearSession(event.accessToken(), event.refreshToken());
            markWithdrawn(event.userId());
            evictUserCaches(event.userId());
        } catch (Exception e) {
            log.error("세션 초기화 실패 userId={}", event.userId(), e);
        }

    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(WithdrawalCancelledEvent event) {
        redisService.deleteKey(TokenKeys.withdrawnKey(event.userId()));
    }

    private void clearSession(String accessToken, String refreshToken) {
        long remainTime = jwtProvider.getExpiration(accessToken);
        if (remainTime > 0) {
            redisService.save(TokenKeys.blacklistKey(accessToken), "logout", remainTime);
        }
        redisService.deleteKey(TokenKeys.refreshKey(refreshToken));
    }

    /**
     * 탈퇴 마커를 남겨, 탈퇴 시점에 다른 기기에 남아 있던 access token으로는 더 이상 API를 호출하지 못하게 한다.
     * 블랙리스트는 탈퇴 요청에 쓰인 토큰 하나만 담기 때문에 userId 단위 마커가 따로 필요하다.
     */
    private void markWithdrawn(Long userId) {
        Duration ttl = Duration.ofDays(retentionDays).plus(WITHDRAWN_MARKER_MARGIN);
        redisService.save(TokenKeys.withdrawnKey(userId), "withdrawn", ttl.toMillis());
    }

    /** 근처 찜 알림이 참조하는 캐시를 즉시 비운다. 남겨두면 유예기간 동안 탈퇴 회원 기준으로 알림 판정이 계속 돈다. */
    private void evictUserCaches(Long userId) {
        activeCourseCacheWriter.evict(userId);
        redisService.deleteKey(LikedSpotsGeoCache.redisKey(userId));
    }
}
