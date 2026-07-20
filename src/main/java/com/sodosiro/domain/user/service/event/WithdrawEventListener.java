package com.sodosiro.domain.user.service.event;

import com.sodosiro.domain.auth.oauth.validator.SocialVerifier;
import com.sodosiro.domain.jwt.JwtProvider;
import com.sodosiro.global.service.RedisService;
import com.sodosiro.global.utils.TokenKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class WithdrawEventListener {

    private final List<SocialVerifier> socialVerifiers;
    private final RedisService redisService;
    private final JwtProvider jwtProvider;

    @Async("withdrawExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(WithdrawEvent event) {
        try {
            clearSession(event.accessToken(), event.refreshToken());
        } catch (Exception e) {
            log.error("세션 초기화 실패 userId={}", event.userId(), e);
        }

        for (WithdrawEvent.SocialInfo social : event.socials()) {
            socialVerifiers.stream()
                    .filter(v -> v.getProvider() == social.provider())
                    .findFirst()
                    .ifPresent(verifier -> {
                        try {
                            verifier.unlink(social.providerId(), social.refreshToken());
                        } catch (Exception e) {
                            log.error("unlink 실패 userId={}, provider={}", event.userId(), social.provider(), e);                        }
                    });
        }
    }

    private void clearSession(String accessToken, String refreshToken) {
        long remainTime = jwtProvider.getExpiration(accessToken);
        if (remainTime > 0) {
            redisService.save(TokenKeys.blacklistKey(accessToken), "logout", remainTime);
        }
        redisService.deleteKey(TokenKeys.refreshKey(refreshToken));
    }
}
