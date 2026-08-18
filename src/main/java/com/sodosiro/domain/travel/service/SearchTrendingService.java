package com.sodosiro.domain.travel.service;

import com.sodosiro.domain.travel.controller.dto.TrendingKeywordResponse;
import com.sodosiro.global.service.RedisService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class SearchTrendingService {

    private static final DateTimeFormatter BUCKET_DATE = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String BUCKET_PREFIX = "travel:search:trending:";
    private static final String UNION_CACHE_KEY = "travel:search:trending:top";
    private static final String DEDUP_PREFIX = "travel:search:dedup:";

    private static final int WINDOW_DAYS = 30;
    private static final int TOP_N = 10;
    private static final int MAX_KEYWORD_LENGTH = 30;

    private static final long BUCKET_TTL_MILLIS = Duration.ofDays(WINDOW_DAYS + 1L).toMillis();
    private static final long UNION_CACHE_TTL_MILLIS = Duration.ofSeconds(60).toMillis();
    private static final long DEDUP_TTL_MILLIS = Duration.ofSeconds(60).toMillis();

    private final RedisService redisService;

    public void countKeyword(String rawKeyword, Long userId, boolean bot) {
        String keyword = normalize(rawKeyword);
        if (keyword == null) {
            return;
        }
        // 내부 봇(시드 봇)은 dedup 을 건너뛰고 무조건 집계한다.
        if (!bot && userId != null
                && !redisService.setIfAbsent(DEDUP_PREFIX + userId + ":" + keyword, "1", DEDUP_TTL_MILLIS)) {
            return; // 단기 TTL 내 동일 사용자·동일 검색어 → 중복 카운트 차단
        }
        String bucketKey = BUCKET_PREFIX + LocalDate.now().format(BUCKET_DATE);
        redisService.incrementScore(bucketKey, keyword, 1D);
        redisService.expire(bucketKey, BUCKET_TTL_MILLIS);
    }

    public List<TrendingKeywordResponse> getTrending() {
        if (!redisService.hasKey(UNION_CACHE_KEY)) {
            recomputeUnion();
        }
        Set<ZSetOperations.TypedTuple<String>> ranked =
                redisService.reverseRangeWithScores(UNION_CACHE_KEY, 0, TOP_N - 1L);
        if (ranked == null || ranked.isEmpty()) {
            return List.of();
        }
        List<TrendingKeywordResponse> result = new ArrayList<>(ranked.size());
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : ranked) {
            if (tuple.getValue() == null) {
                continue;
            }
            long count = tuple.getScore() == null ? 0L : Math.round(tuple.getScore());
            result.add(TrendingKeywordResponse.of(rank++, tuple.getValue(), count));
        }
        return result;
    }

    private void recomputeUnion() {
        LocalDate today = LocalDate.now();
        List<String> bucketKeys = new ArrayList<>(WINDOW_DAYS);
        for (int i = 0; i < WINDOW_DAYS; i++) {
            bucketKeys.add(BUCKET_PREFIX + today.minusDays(i).format(BUCKET_DATE));
        }
        redisService.zUnionAndStore(bucketKeys.getFirst(), bucketKeys.subList(1, bucketKeys.size()), UNION_CACHE_KEY);
        redisService.expire(UNION_CACHE_KEY, UNION_CACHE_TTL_MILLIS);
    }

    private String normalize(String raw) {
        if (raw == null) {
            return null;
        }
        String normalized = raw.trim().toLowerCase().replaceAll("\\s+", " ");
        if (normalized.isEmpty() || normalized.length() > MAX_KEYWORD_LENGTH) {
            return null;
        }
        return normalized;
    }
}
