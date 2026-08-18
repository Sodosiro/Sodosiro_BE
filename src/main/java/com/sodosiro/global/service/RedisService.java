package com.sodosiro.global.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public String getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    public void deleteKey(String key) {
        redisTemplate.delete(key);
    }

    public void save(String key, String value, long durationMillis) {
        redisTemplate.opsForValue().set(key, value, Duration.ofMillis(durationMillis));
    }

    public boolean hasKey(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    /**
     * key 가 없을 때만 value 를 세팅하고 TTL 을 건다(SET NX PX). 세팅에 성공하면 true.
     * 짧은 TTL 로 중복 이벤트(어뷰징) 차단에 사용한다.
     */
    public boolean setIfAbsent(String key, String value, long durationMillis) {
        return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(key, value, Duration.ofMillis(durationMillis)));
    }

    /** Sorted Set member 의 score 를 delta 만큼 증가시키고 갱신된 score 를 반환한다(ZINCRBY). */
    public Double incrementScore(String key, String member, double delta) {
        return redisTemplate.opsForZSet().incrementScore(key, member, delta);
    }

    public void expire(String key, long durationMillis) {
        redisTemplate.expire(key, Duration.ofMillis(durationMillis));
    }

    /** key + otherKeys 의 Sorted Set 을 SUM 으로 합쳐 destKey 에 저장한다(ZUNIONSTORE). */
    public void zUnionAndStore(String key, Collection<String> otherKeys, String destKey) {
        redisTemplate.opsForZSet().unionAndStore(key, otherKeys, destKey);
    }

    /** score 내림차순 [start, end] 구간을 score 와 함께 반환한다(ZREVRANGE WITHSCORES). */
    public Set<ZSetOperations.TypedTuple<String>> reverseRangeWithScores(String key, long start, long end) {
        return redisTemplate.opsForZSet().reverseRangeWithScores(key, start, end);
    }

}