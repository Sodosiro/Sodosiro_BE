package com.sodosiro.domain.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodosiro.domain.course.controller.dto.CourseConfirmRequest;
import com.sodosiro.domain.course.controller.dto.DayConfirm;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.repository.CourseRepository;
import com.sodosiro.domain.course.service.dto.ActiveCourseCache;
import com.sodosiro.domain.route.dto.RouteWaypoint;
import com.sodosiro.domain.route.constants.TransportMode;
import com.sodosiro.domain.route.service.AdjacentRouteResult;
import com.sodosiro.domain.route.service.RouteCalculationService;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.payload.code.error.CourseErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import com.sodosiro.global.service.RedisService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 프론트에서 확정한 일자별 관광지 순서에 이동수단별 구간 데이터를 붙여 응답한다.
 * courseId로 draft(Course)를 찾아 확정 상태로 갱신하되, 구간(경로) 데이터 자체는 저장하지 않는다.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CourseConfirmationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TouristSpotRepository touristSpotRepository;
    private final RouteCalculationService routeCalculationService;
    private final CourseRepository courseRepository;
    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    /** 확정 전 draft의 제목과 일자별 관광지 순서를 수정한다. title이 없으면 기존 제목을 유지한다. */
    public void updateDraftDays(Long userId, Long courseId, String title, List<DayConfirm> days) {

        Course course = courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new GeneralException(CourseErrorCode._COURSE_NOT_FOUND));

        if (course.getIsConfirmed()) {
            throw new GeneralException(CourseErrorCode._COURSE_ALREADY_CONFIRMED);
        }

        if (title != null && !title.isBlank()) {
            course.updateTitle(title);
        }

        Map<Long, TouristSpot> spotsById = findSpotsByContentId(days);

        Map<Integer, LocalDate> datesByDay = course.getDays().stream()
                .collect(Collectors.toMap(Course.DaySnapshot::day, Course.DaySnapshot::date));

        List<Course.DaySnapshot> rebuiltDays = days.stream()
                .map(dayConfirm -> toSnapshot(dayConfirm, spotsById, course.getMustVisitContentId(), datesByDay))
                .toList();

        course.updateDays(rebuiltDays);
    }

    /**
     * draft에 이미 저장된 transportMode/days를 그대로 사용해 코스를 확정한다.
     * 요청은 courseId만 받으며, 어떤 카카오 API를 호출할지는 draft의 transportMode로 결정한다.
     * 계산된 경로는 저장만 하고 응답으로 돌려주지 않는다 — GET /courses/{courseId}로 조회한다.
     */
    public void confirm(Long userId, CourseConfirmRequest request) {

        Course course = courseRepository.findByIdAndUserId(request.courseId(), userId)
                .orElseThrow(() -> new GeneralException(CourseErrorCode._COURSE_NOT_FOUND));

        TransportMode transportMode = course.getTransportMode();
        if (transportMode == null) {
            throw new GeneralException(CourseErrorCode._TRANSPORT_MODE_NOT_SELECTED);
        }

        if (transportMode == TransportMode.CAR) {
            List<Course.DayCarRoute> days = course.getDays().stream()
                    .map(day -> new Course.DayCarRoute(day.day(), calculateCarLegs(toWaypointsFromSnapshot(day.spots()))))
                    .toList();
            course.updateCarRoutes(days);
        } else {
            List<Course.DayPublicTransportRoute> days = course.getDays().stream()
                    .map(day -> new Course.DayPublicTransportRoute(day.day(), calculatePublicTransportDetails(toWaypointsFromSnapshot(day.spots()))))
                    .toList();
            course.updateTransitRoutes(days);
        }
        course.confirmDraft();
        cacheActiveCourse(userId, course);
    }

    private List<RouteWaypoint> toWaypointsFromSnapshot(List<Course.SpotSnapshot> spots) {
        return spots.stream()
                .map(spot -> new RouteWaypoint(spot.contentId(), spot.mapX(), spot.mapY()))
                .toList();
    }

    private void cacheActiveCourse(Long userId, Course course) {
        List<Long> scheduledContentIds = course.allSpots().stream()
                .map(Course.SpotSnapshot::contentId)
                .toList();
        ActiveCourseCache cache = new ActiveCourseCache(
                course.getId(), course.getStartDate(), course.getEndDate(), scheduledContentIds);

        try {
            String json = objectMapper.writeValueAsString(cache);
            ZonedDateTime expiresAt = course.getEndDate().plusDays(1).atStartOfDay(KST);
            long ttlMillis = Duration.between(ZonedDateTime.now(KST), expiresAt).toMillis();
            redisService.save(ActiveCourseCache.redisKey(userId), json, Math.max(1, ttlMillis));
        } catch (JsonProcessingException exception) {
            log.warn("활성 코스 캐시 저장에 실패했습니다. userId={}, courseId={}", userId, course.getId(), exception);
        }
    }

    private Course.DaySnapshot toSnapshot(DayConfirm dayConfirm, Map<Long, TouristSpot> spotsById, Long mustVisitContentId, Map<Integer, LocalDate> datesByDay) {

        List<Course.SpotSnapshot> spots = dayConfirm.contentIds().stream()
                .map(spotsById::get)
                .map(spot -> new Course.SpotSnapshot(
                        spot.getContentId(), spot.getTitle(), spot.getFirstImage(),
                        spot.getMapX(), spot.getMapY(), spot.getCategory(),
                        spot.getContentId().equals(mustVisitContentId)))
                .toList();
        return new Course.DaySnapshot(dayConfirm.day(), datesByDay.get(dayConfirm.day()), spots);
    }

    private List<com.sodosiro.domain.route.dto.RouteLeg> calculateCarLegs(List<RouteWaypoint> waypoints) {
        AdjacentRouteResult result = routeCalculationService.calculateAdjacentRoutes(waypoints, TransportMode.CAR);
        if (result instanceof AdjacentRouteResult.Car car) {
            return car.legs();
        }
        throw new IllegalStateException("예상하지 못한 경로 계산 결과 타입입니다.");
    }

    private List<com.sodosiro.domain.route.kakao.dto.KakaoTransitRouteResult> calculatePublicTransportDetails(List<RouteWaypoint> waypoints) {

        AdjacentRouteResult result = routeCalculationService.calculateAdjacentRoutes(waypoints, TransportMode.PUBLIC_TRANSPORT);

        if (result instanceof AdjacentRouteResult.PublicTransport publicTransport) {
            return publicTransport.details();
        }
        throw new IllegalStateException("예상하지 못한 경로 계산 결과 타입입니다.");
    }

    private Map<Long, TouristSpot> findSpotsByContentId(List<DayConfirm> days) {

        List<Long> contentIds = days.stream()
                .flatMap(day -> day.contentIds().stream())
                .distinct()
                .toList();

        List<TouristSpot> spots = touristSpotRepository.findAllById(contentIds);
        if (spots.size() != contentIds.size()) {
            throw new GeneralException(CourseErrorCode._CONTENT_NOT_FOUND);
        }
        return spots.stream().collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));
    }
}