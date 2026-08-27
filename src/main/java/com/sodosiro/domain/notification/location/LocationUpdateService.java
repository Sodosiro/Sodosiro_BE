package com.sodosiro.domain.notification.location;

import com.sodosiro.domain.course.service.dto.ActiveCourseCache;
import com.sodosiro.domain.gps.entity.Gps;
import com.sodosiro.domain.gps.repository.GpsRepository;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.payload.code.error.LocationErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import com.sodosiro.global.service.RedisService;
import com.sodosiro.global.utils.TimeZones;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Service;

import static java.util.stream.Collectors.toMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class LocationUpdateService {

    private static final double SEARCH_RADIUS_KILOMETERS = 0.2; // 알림 범위 현재는 200m
    private static final double MAX_ACCURACY_METERS = 100.0;    // 알림 정확도
    private static final Duration MAX_EVENT_AGE = Duration.ofMinutes(5);

    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final GpsRepository gpsRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final LocationNotificationService locationNotificationService;

    @Value("${notification.nearby.redis-ttl-seconds:300}")
    private long nearbySpotsTtlSeconds;

    @Value("${notification.nearby.minimum-interval-hours:4}")
    private long minimumIntervalHours;

    @Value("${notification.nearby.daily-max-notifications:2}")
    private int dailyMaxNotifications;

    public LocationUpdateResult process(Long userId, double latitude, double longitude, double accuracy, Instant occurredAt) {
        validate(accuracy, occurredAt);

        ActiveCourseCache course = findActiveCourse(userId);
        if (course == null) {
            return LocationUpdateResult.ignored("COURSE_NOT_IN_PROGRESS");
        }

        Set<Long> excludedContentIds = excludedContentIds(course);
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> nearbySpots = redisService.searchGeo(
                "user:%d:liked-spots:geo".formatted(userId),
                longitude,
                latitude,
                SEARCH_RADIUS_KILOMETERS
        );
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> eligibleNearbySpots = nearbySpots.stream()
                .filter(result -> !excludedContentIds.contains(Long.valueOf(result.getContent().getName())))
                .toList();
        List<String> nearbyContentIds = eligibleNearbySpots.stream()
                .map(result -> result.getContent().getName())
                .toList();
        List<String> newlyEnteredContentIds = redisService.replaceNearbySpotsAndFindNewEntries(
                "user:%d:course:%d:nearby-spots".formatted(userId, course.courseId()),
                nearbyContentIds,
                nearbySpotsTtlSeconds
        );

        boolean notificationTriggered = createNearbyNotification(userId, course, eligibleNearbySpots, newlyEnteredContentIds);
        return LocationUpdateResult.processed(eligibleNearbySpots.size(), newlyEnteredContentIds, notificationTriggered);
    }

    private void validate(double accuracy, Instant occurredAt) {
        if (accuracy < 0 || accuracy > MAX_ACCURACY_METERS) {
            throw new GeneralException(LocationErrorCode._LOW_LOCATION_ACCURACY);
        }
        if (occurredAt.isBefore(Instant.now().minus(MAX_EVENT_AGE))) {
            throw new GeneralException(LocationErrorCode._STALE_LOCATION_EVENT);
        }
    }

    private boolean createNearbyNotification(
            Long userId,
            ActiveCourseCache course,
            List<GeoResult<RedisGeoCommands.GeoLocation<String>>> nearbySpots,
            List<String> newlyEnteredContentIds) {
        if (newlyEnteredContentIds.isEmpty() || nearbySpots.isEmpty()) {
            return false;
        }

        Set<String> newlyEntered = new HashSet<>(newlyEnteredContentIds);
        List<Long> newlyEnteredNearbyContentIds = nearbySpots.stream()
                .map(result -> result.getContent().getName())
                .filter(newlyEntered::contains)
                .map(Long::valueOf)
                .toList();
        if (newlyEnteredNearbyContentIds.isEmpty()) {
            return false;
        }

        // 거리순(가까운 것부터) contentId 목록 → 제목 조회
        List<Long> nearbyContentIds = nearbySpots.stream()
                .map(result -> Long.valueOf(result.getContent().getName()))
                .toList();

        Map<Long, String> titleById = touristSpotRepository.findAllById(nearbyContentIds).stream()
                .collect(toMap(TouristSpot::getContentId, TouristSpot::getTitle));

        Long nearestContentId = newlyEnteredNearbyContentIds.getFirst();
        String nearestTitle = titleById.get(nearestContentId);
        if (nearestTitle == null) {
            log.warn("근처 알림 대상 관광지를 찾지 못했습니다. userId={}, contentId={}", userId, nearestContentId);
            return false;
        }

        // 거리순으로 제목 정렬(조회 못한 것은 제외)
        List<String> nearbyTitles = nearbyContentIds.stream()
                .map(titleById::get)
                .filter(Objects::nonNull)
                .toList();

        RedisService.NearbyNotificationPermit permit = reserveNotificationPermit(
                userId, course.courseId(), nearestContentId, course.endDate());
        if (permit != RedisService.NearbyNotificationPermit.ACQUIRED) {
            log.debug("근처 찜 알림 발송 제한 userId={} courseId={} contentId={} reason={}",
                    userId, course.courseId(), nearestContentId, permit);
            return false;
        }

        return locationNotificationService.createNearbyNotification(
                userId,
                course.courseId(),
                course.endDate(),
                nearestContentId,
                nearestTitle,
                nearbySpots.size(),
                nearbyTitles,
                nearbyContentIds
        );
    }

    private ActiveCourseCache findActiveCourse(Long userId) {
        String json = redisService.getValue(ActiveCourseCache.redisKey(userId));
        if (json == null) {
            return null;
        }

        ActiveCourseCache course;
        try {
            course = objectMapper.readValue(json, ActiveCourseCache.class);
        } catch (Exception exception) {
            log.warn("활성 코스 캐시 역직렬화에 실패했습니다. userId={}", userId, exception);
            return null;
        }

        LocalDate today = LocalDate.now(TimeZones.KST);
        return today.isBefore(course.startDate()) || today.isAfter(course.endDate()) ? null : course;
    }

    private Set<Long> excludedContentIds(ActiveCourseCache course) {
        Set<Long> excluded = new HashSet<>(course.scheduledContentIds());
        gpsRepository.findByCourseId(course.courseId()).stream()
                .map(Gps::getContentId)
                .forEach(excluded::add);
        return excluded;
    }

    private RedisService.NearbyNotificationPermit reserveNotificationPermit(
            Long userId, Long courseId, Long contentId, LocalDate courseEndDate) {
        ZonedDateTime now = ZonedDateTime.now(TimeZones.KST);
        ZonedDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(TimeZones.KST);
        ZonedDateTime courseExpiresAt = courseEndDate.plusDays(1).atStartOfDay(TimeZones.KST);
        return redisService.reserveNearbyNotification(
                "user:%d:course:%d:nearby-notification:spot:%d".formatted(userId, courseId, contentId),
                "user:%d:nearby-notification:daily:%s".formatted(userId, now.toLocalDate()),
                "user:%d:nearby-notification:last-sent".formatted(userId),
                now.toInstant().toEpochMilli(),
                Math.max(1, Duration.between(now, courseExpiresAt).toMillis()),
                Math.max(1, Duration.between(now, nextMidnight).toMillis()),
                Duration.ofHours(minimumIntervalHours).toMillis(),
                dailyMaxNotifications);
    }
}
