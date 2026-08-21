package com.sodosiro.domain.notification.location;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.repository.CourseRepository;
import com.sodosiro.domain.gps.entity.Gps;
import com.sodosiro.domain.gps.repository.GpsRepository;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.service.RedisService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import static java.util.stream.Collectors.toMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class LocationWebSocketHandler extends TextWebSocketHandler {

    private static final double SEARCH_RADIUS_KILOMETERS = 0.2; // 알림 범위 현재는 200m
    private static final double MAX_ACCURACY_METERS = 100.0;    // 알림 정확도
    private static final Duration MAX_EVENT_AGE = Duration.ofMinutes(5);
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ObjectMapper objectMapper;
    private final RedisService redisService;
    private final CourseRepository courseRepository;
    private final GpsRepository gpsRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final LocationNotificationService locationNotificationService;

    @Value("${notification.nearby.redis-ttl-seconds:300}")
    private long nearbySpotsTtlSeconds;

    @Value("${notification.nearby.minimum-interval-hours:4}")
    private long minimumIntervalHours;

    @Value("${notification.nearby.daily-max-notifications:2}")
    private int dailyMaxNotifications;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = (Long) session.getAttributes().get("userId");
        log.info("WebSocket 연결됨. sessionId={}, userId={}, remote={}",
                session.getId(), userId, session.getRemoteAddress());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = (Long) session.getAttributes().get("userId");
        log.info("WebSocket 연결 끊김. sessionId={}, userId={}, closeCode={}, reason={}",
                session.getId(), userId, status.getCode(), status.getReason());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        Long userId = (Long) session.getAttributes().get("userId");
        log.warn("WebSocket 전송 오류. sessionId={}, userId={}, error={}",
                session.getId(), userId, exception.getMessage());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Long userId = (Long) session.getAttributes().get("userId");
        if (userId == null) {
            sendError(session, "UNAUTHORIZED", "인증 정보를 찾을 수 없습니다.");
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        try {
            LocationUpdateRequest request = objectMapper.readValue(message.getPayload(), LocationUpdateRequest.class);
            if (!isValid(request)) {
                sendError(session, "INVALID_LOCATION_UPDATE", "위치 메시지 형식 또는 값이 유효하지 않습니다.");
                return;
            }

            processLocationUpdate(session, userId, request);
        } catch (Exception exception) {
            log.warn("위치 메시지 처리에 실패했습니다. userId={}", userId, exception);
            sendError(session, "LOCATION_PROCESSING_FAILED", "위치 메시지를 처리하지 못했습니다.");
        }
    }

    private void processLocationUpdate(
            WebSocketSession session,
            Long userId,
            LocationUpdateRequest request) throws Exception {
        Course course = findActiveCourse(userId, request.courseId());
        if (course == null) {
            send(session, Map.of(
                    "type", "LOCATION_IGNORED",
                    "reason", "COURSE_NOT_IN_PROGRESS",
                    "notificationTriggered", false));
            return;
        }

        Set<Long> excludedContentIds = excludedContentIds(course);
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> nearbySpots = redisService.searchGeo(
                "user:%d:liked-spots:geo".formatted(userId),
                request.longitude(),
                request.latitude(),
                SEARCH_RADIUS_KILOMETERS
        );
        List<GeoResult<RedisGeoCommands.GeoLocation<String>>> eligibleNearbySpots = nearbySpots.stream()
                .filter(result -> !excludedContentIds.contains(Long.valueOf(result.getContent().getName())))
                .toList();
        List<String> nearbyContentIds = eligibleNearbySpots.stream()
                .map(result -> result.getContent().getName())
                .toList();
        List<String> newlyEnteredContentIds = redisService.replaceNearbySpotsAndFindNewEntries(
                "user:%d:course:%d:nearby-spots".formatted(userId, course.getId()),
                nearbyContentIds,
                nearbySpotsTtlSeconds
        );

        send(session, Map.of(
                "type", "LOCATION_PROCESSED",
                "nearbyCount", eligibleNearbySpots.size(),
                "newlyEnteredContentIds", newlyEnteredContentIds,
                "notificationTriggered", createNearbyNotification(
                        userId, course, eligibleNearbySpots, newlyEnteredContentIds)
        ));
    }

    private boolean createNearbyNotification(
            Long userId,
            Course course,
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
                .filter(java.util.Objects::nonNull)
                .toList();

        RedisService.NearbyNotificationPermit permit = reserveNotificationPermit(
                userId, course.getId(), nearestContentId, course.getEndDate());
        if (permit != RedisService.NearbyNotificationPermit.ACQUIRED) {
            log.debug("근처 찜 알림 발송 제한 userId={} courseId={} contentId={} reason={}",
                    userId, course.getId(), nearestContentId, permit);
            return false;
        }

        return locationNotificationService.createNearbyNotification(
                userId,
                course.getId(),
                course.getEndDate(),
                nearestContentId,
                nearestTitle,
                nearbySpots.size(),
                nearbyTitles
        );
    }

    private void sendError(WebSocketSession session, String code, String message) throws Exception {
        send(session, Map.of(
                "type", "LOCATION_ERROR",
                "code", code,
                "message", message
        ));
    }

    private void send(WebSocketSession session, Map<String, Object> payload) throws Exception {
        if (session.isOpen()) {
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(payload)));
        }
    }

    private boolean isValid(LocationUpdateRequest request) {
        if (request == null || !"LOCATION_UPDATE".equals(request.type())) {
            return false;
        }
        if (request.courseId() == null
                || request.latitude() == null || request.longitude() == null
                || request.accuracy() == null || request.occurredAt() == null) {
            return false;
        }
        if (request.latitude() < -90 || request.latitude() > 90 || request.longitude() < -180 || request.longitude() > 180) {
            return false;
        }
        if (request.accuracy() < 0 || request.accuracy() > MAX_ACCURACY_METERS) {
            return false;
        }
        return !request.occurredAt().isBefore(Instant.now().minus(MAX_EVENT_AGE));
    }

    private Course findActiveCourse(Long userId, Long courseId) {
        Course course = courseRepository.findByIdAndUserId(courseId, userId).orElse(null);
        if (course == null || !Boolean.TRUE.equals(course.getIsConfirmed())
                || course.getStatus() != CourseStatus.IN_PROGRESS) {
            return null;
        }
        LocalDate today = LocalDate.now(KST);
        return today.isBefore(course.getStartDate()) || today.isAfter(course.getEndDate()) ? null : course;
    }

    private Set<Long> excludedContentIds(Course course) {
        Set<Long> excluded = course.allSpots().stream()
                .map(Course.SpotSnapshot::contentId)
                .collect(java.util.stream.Collectors.toSet());
        gpsRepository.findByCourseId(course.getId()).stream()
                .map(Gps::getContentId)
                .forEach(excluded::add);
        return excluded;
    }

    private RedisService.NearbyNotificationPermit reserveNotificationPermit(
            Long userId, Long courseId, Long contentId, LocalDate courseEndDate) {
        ZonedDateTime now = ZonedDateTime.now(KST);
        ZonedDateTime nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(KST);
        ZonedDateTime courseExpiresAt = courseEndDate.plusDays(1).atStartOfDay(KST);
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
