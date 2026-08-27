package com.sodosiro.global.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.domain.geo.GeoReference;
import org.springframework.data.geo.Metrics;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collection;
import java.util.Set;
import java.util.List;

@Slf4j
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

    public void addGeo(String key, double longitude, double latitude, String member) {
        redisTemplate.opsForGeo().add(key, new Point(longitude, latitude), member);
    }

    public void removeFromGeo(String key, String member) {
        redisTemplate.opsForZSet().remove(key, member);
    }

    public List<GeoResult<RedisGeoCommands.GeoLocation<String>>> searchGeo(
            String key,
            double longitude,
            double latitude,
            double radiusKilometers) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = redisTemplate.opsForGeo().search(
                key,
                GeoReference.fromCoordinate(longitude, latitude),
                new Distance(radiusKilometers, Metrics.KILOMETERS),
                RedisGeoCommands.GeoSearchCommandArgs.newGeoSearchArgs()
                        .includeDistance()
                        .sortAscending()
        );

        log.info("searchGeo: {}", results);

        return results == null ? List.of() : results.getContent();
    }

    public List<String> replaceNearbySpotsAndFindNewEntries(String key, List<String> currentSpotIds, long ttlSeconds) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setResultType(List.class);
        script.setScriptText("""
                local ttl = tonumber(ARGV[1])
                local previous = redis.call('SMEMBERS', KEYS[1])
                local known = {}
                for _, member in ipairs(previous) do known[member] = true end
                redis.call('DEL', KEYS[1])
                local entered = {}
                for i = 2, #ARGV do
                    local member = ARGV[i]
                    redis.call('SADD', KEYS[1], member)
                    if not known[member] then table.insert(entered, member) end
                end
                if redis.call('EXISTS', KEYS[1]) == 1 then
                    redis.call('EXPIRE', KEYS[1], ttl)
                end
                return entered
                """);

        Object[] args = new Object[currentSpotIds.size() + 1];
        args[0] = String.valueOf(ttlSeconds);
        for (int i = 0; i < currentSpotIds.size(); i++) {
            args[i + 1] = currentSpotIds.get(i);
        }

        List<String> entered = redisTemplate.execute(script, List.of(key), args);
        return entered == null ? List.of() : entered.stream().map(String::valueOf).toList();
    }

    public NearbyNotificationPermit reserveNearbyNotification(
            String sentSpotKey,
            String dailyCountKey,
            String lastSentKey,
            long nowEpochMillis,
            long sentSpotTtlMillis,
            long dailyTtlMillis,
            long minimumIntervalMillis,
            int dailyLimit) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                if redis.call('EXISTS', KEYS[1]) == 1 then return 0 end
                if redis.call('EXISTS', KEYS[3]) == 1 then return 1 end
                local count = tonumber(redis.call('GET', KEYS[2]) or '0')
                if count >= tonumber(ARGV[4]) then return 2 end
                redis.call('SET', KEYS[1], '1', 'PX', ARGV[2])
                redis.call('SET', KEYS[2], tostring(count + 1), 'PX', ARGV[3])
                redis.call('SET', KEYS[3], ARGV[1], 'PX', ARGV[5])
                return 3
                """);
        Long result = redisTemplate.execute(
                script,
                List.of(sentSpotKey, dailyCountKey, lastSentKey),
                String.valueOf(nowEpochMillis),
                String.valueOf(sentSpotTtlMillis),
                String.valueOf(dailyTtlMillis),
                String.valueOf(dailyLimit),
                String.valueOf(minimumIntervalMillis));
        return switch (result == null ? -1 : result.intValue()) {
            case 3 -> NearbyNotificationPermit.ACQUIRED;
            case 0 -> NearbyNotificationPermit.SAME_SPOT_ALREADY_SENT;
            case 1 -> NearbyNotificationPermit.MINIMUM_INTERVAL;
            case 2 -> NearbyNotificationPermit.DAILY_LIMIT;
            default -> NearbyNotificationPermit.REJECTED;
        };
    }

    public enum NearbyNotificationPermit {
        ACQUIRED,
        SAME_SPOT_ALREADY_SENT,
        MINIMUM_INTERVAL,
        DAILY_LIMIT,
        REJECTED
    }

    /**
     * key 의 카운트가 limit 미만이면 원자적으로 증가시키고 true, limit 이상이면 증가시키지 않고 false 를 반환한다.
     * 첫 증가(count == 1) 시점에만 TTL 을 걸어 자정 등 기준 시점에 자동 만료되게 한다.
     */
    public boolean tryConsumeDailyQuota(String key, int limit, long ttlSeconds) {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setResultType(Long.class);
        script.setScriptText("""
                local count = tonumber(redis.call('GET', KEYS[1]) or '0')
                if count >= tonumber(ARGV[1]) then return 0 end
                local newCount = redis.call('INCR', KEYS[1])
                if newCount == 1 then
                    redis.call('EXPIRE', KEYS[1], ARGV[2])
                end
                return 1
                """);
        Long result = redisTemplate.execute(script, List.of(key), String.valueOf(limit), String.valueOf(ttlSeconds));
        return result != null && result == 1L;
    }

}
