package com.sodosiro.domain.course.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.service.dto.ActiveCourseCache;
import com.sodosiro.global.service.RedisService;
import com.sodosiro.global.utils.TimeZones;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class ActiveCourseCacheWriter {

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    /** 진행 중 코스의 일정 스냅샷을 활성 코스 캐시에 기록한다. 여행 종료 다음날 KST 00:00까지 유지된다. */
    public void write(Long userId, Course course) {
        try {
            List<Long> scheduledContentIds = course.allSpots().stream()
                    .map(Course.SpotSnapshot::contentId)
                    .toList();
            ActiveCourseCache cache = new ActiveCourseCache(
                    course.getId(), course.getStartDate(), course.getEndDate(), scheduledContentIds);

            String json = objectMapper.writeValueAsString(cache);
            ZonedDateTime expiresAt = course.getEndDate().plusDays(1).atStartOfDay(TimeZones.KST);
            long ttlMillis = Duration.between(ZonedDateTime.now(TimeZones.KST), expiresAt).toMillis();
            redisService.save(ActiveCourseCache.redisKey(userId), json, Math.max(1, ttlMillis));
        } catch (Exception exception) {
            log.warn("활성 코스 캐시 저장에 실패했습니다. userId={}, courseId={}", userId, course.getId(), exception);
        }
    }

    /** 활성 코스 캐시를 제거한다. 여행 종료·코스 삭제 시 근처 찜 알림이 더는 지난 여행을 참조하지 않도록 한다. */
    public void evict(Long userId) {
        try {
            redisService.deleteKey(ActiveCourseCache.redisKey(userId));
        } catch (Exception exception) {
            log.warn("활성 코스 캐시 삭제에 실패했습니다. userId={}", userId, exception);
        }
    }
}
