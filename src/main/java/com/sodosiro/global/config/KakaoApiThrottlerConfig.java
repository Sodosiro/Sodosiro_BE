package com.sodosiro.global.config;

import com.sodosiro.domain.route.kakao.client.KakaoApiThrottler;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** 카카오맵(대중교통·도보)과 카카오모빌리티(자차)는 호스트·쿼터가 달라 스로틀러를 분리한다. */
@Configuration
public class KakaoApiThrottlerConfig {

    @Bean
    public KakaoApiThrottler kakaoMapApiThrottler(
            @Value("${kakao.map.max-concurrent-calls:5}") int maxConcurrentCalls,
            @Value("${kakao.map.max-retries:2}") int maxRetries,
            @Value("${kakao.map.retry-backoff-ms:200}") long retryBackoffMs) {
        return new KakaoApiThrottler(maxConcurrentCalls, maxRetries, Duration.ofMillis(retryBackoffMs));
    }

    @Bean
    public KakaoApiThrottler kakaoMobilityApiThrottler(
            @Value("${kakao.mobility.max-concurrent-calls:5}") int maxConcurrentCalls,
            @Value("${kakao.mobility.max-retries:2}") int maxRetries,
            @Value("${kakao.mobility.retry-backoff-ms:200}") long retryBackoffMs) {
        return new KakaoApiThrottler(maxConcurrentCalls, maxRetries, Duration.ofMillis(retryBackoffMs));
    }
}
