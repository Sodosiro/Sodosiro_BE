package com.sodosiro.domain.route.kakao.client;

import java.time.Duration;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpClientErrorException;

/**
 * 카카오 API는 초당·동시 호출 제한을 콘솔에도 공개하지 않는다. 정적인 숫자로 한도를 맞히는 대신,
 * (1) 동시 호출 수를 세마포어로 보수적으로 캡해 제한에 부딪히는 빈도 자체를 줄이고,
 * (2) 그래도 제한 초과 응답(code=-10, "API limit has been exceeded")이 오면 짧게 대기 후 재시도해
 * 실제 한도가 얼마든 실시간으로 대응한다. 좌표 오류 등 재시도해도 소용없는 다른 4xx/타임아웃은
 * 그대로 호출부로 전파해 기존 실패 처리(로그 + 빈 결과 반환)를 따르게 한다.
 */
@Slf4j
public class KakaoApiThrottler {

    private static final String RATE_LIMIT_CODE_MARKER = "\"code\":-10";
    private static final String RATE_LIMIT_MESSAGE_MARKER = "API limit has been exceeded";

    private final Semaphore semaphore;
    private final int maxRetries;
    private final Duration initialBackoff;

    public KakaoApiThrottler(int maxConcurrentCalls, int maxRetries, Duration initialBackoff) {
        this.semaphore = new Semaphore(maxConcurrentCalls);
        this.maxRetries = maxRetries;
        this.initialBackoff = initialBackoff;
    }

    /** context는 재시도/성공 로그에 실릴 호출 식별자(예: "walk start=(...), end=(...)")로, 실패 로그와 코스 데이터를 대조할 수 있게 한다. */
    public <T> T execute(Supplier<T> apiCall, String context) {
        return callWithRetry(apiCall, context, 0);
    }

    /** permit은 실제 네트워크 호출 구간에서만 쥐고 있는다 — 백오프 대기 중에는 반납해, 다른 요청이 그 슬롯을 즉시 쓸 수 있게 한다. */
    private <T> T callWithRetry(Supplier<T> apiCall, String context, int attempt) {
        try {
            T result = callOnce(apiCall);
            log.debug("카카오 API 호출 성공({}), {}번째 시도", context, attempt + 1);
            return result;
        } catch (HttpClientErrorException.BadRequest exception) {
            if (attempt >= maxRetries || !isRateLimited(exception)) {
                throw exception;
            }
            long backoffMillis = initialBackoff.toMillis() * (1L << attempt);
            log.warn("카카오 API 호출 제한 감지({}), {}ms 후 재시도({}/{})", context, backoffMillis, attempt + 1, maxRetries);
            sleep(backoffMillis);
            return callWithRetry(apiCall, context, attempt + 1);
        }
    }

    private <T> T callOnce(Supplier<T> apiCall) {
        acquireUninterruptibly();
        try {
            return apiCall.get();
        } finally {
            semaphore.release();
        }
    }

    private boolean isRateLimited(HttpClientErrorException.BadRequest exception) {
        String body = exception.getResponseBodyAsString();
        return body != null && (body.contains(RATE_LIMIT_CODE_MARKER) || body.contains(RATE_LIMIT_MESSAGE_MARKER));
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void acquireUninterruptibly() {
        try {
            semaphore.acquire();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("카카오 API 호출 대기 중 인터럽트됨", exception);
        }
    }
}
