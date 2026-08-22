package com.sodosiro.domain.course.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodosiro.domain.course.controller.dto.CourseConfirmCarRequest;
import com.sodosiro.domain.course.controller.dto.CourseConfirmCarResponse;
import com.sodosiro.domain.course.controller.dto.CourseConfirmPublicTransportRequest;
import com.sodosiro.domain.course.controller.dto.CourseConfirmPublicTransportResponse;
import com.sodosiro.domain.course.controller.dto.DayConfirm;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.repository.CourseRepository;
import com.sodosiro.domain.course.service.dto.ActiveCourseCache;
import com.sodosiro.domain.route.dto.RouteWaypoint;
import com.sodosiro.domain.route.dto.TransportMode;
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

    public CourseConfirmCarResponse confirmCar(Long userId, CourseConfirmCarRequest request) {

        Map<Long, TouristSpot> spotsById = confirmCourse(userId, request.courseId(), request.days(), TransportMode.CAR);

        List<CourseConfirmCarResponse.DayCarRoute> days = request.days().stream()
                .map(dayConfirm -> new CourseConfirmCarResponse.DayCarRoute(dayConfirm.day(), calculateCarLegs(toWaypoints(dayConfirm, spotsById))))
                .toList();
        return new CourseConfirmCarResponse(days);
    }

    public CourseConfirmPublicTransportResponse confirmPublicTransport(Long userId, CourseConfirmPublicTransportRequest request) {

        Map<Long, TouristSpot> spotsById = confirmCourse(userId, request.courseId(), request.days(), TransportMode.PUBLIC_TRANSPORT);

        List<CourseConfirmPublicTransportResponse.DayPublicTransportRoute> days = request.days().stream()
                .map(dayConfirm -> new CourseConfirmPublicTransportResponse.DayPublicTransportRoute(
                        dayConfirm.day(), calculatePublicTransportDetails(toWaypoints(dayConfirm, spotsById))))
                .toList();
        return new CourseConfirmPublicTransportResponse(days);
    }

    /** draft 소유자 검증 후 제출된 최종 장소/순서로 draft를 확정 상태로 덮어쓴다. */
    private Map<Long, TouristSpot> confirmCourse(Long userId, Long courseId, List<DayConfirm> days, TransportMode transportMode) {

        Course course = courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new GeneralException(CourseErrorCode._COURSE_NOT_FOUND));

        Map<Long, TouristSpot> spotsById = findSpotsByContentId(days);
        Map<Integer, LocalDate> datesByDay = course.getDays().stream()
                .collect(Collectors.toMap(Course.DaySnapshot::day, Course.DaySnapshot::date));

        List<Course.DaySnapshot> rebuiltDays = days.stream()
                .map(dayConfirm -> toSnapshot(dayConfirm, spotsById, course.getMustVisitContentId(), datesByDay))
                .toList();

        course.confirm(transportMode, rebuiltDays);
        cacheActiveCourse(userId, course);
        return spotsById;
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

    private List<RouteWaypoint> toWaypoints(DayConfirm dayConfirm, Map<Long, TouristSpot> spotsById) {
        return dayConfirm.contentIds().stream()
                .map(spotsById::get)
                .map(spot -> new RouteWaypoint(spot.getContentId(), spot.getMapX(), spot.getMapY()))
                .toList();
    }
}