package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.controller.dto.CourseConfirmCarRequest;
import com.sodosiro.domain.course.controller.dto.CourseConfirmCarResponse;
import com.sodosiro.domain.course.controller.dto.CourseConfirmPublicTransportRequest;
import com.sodosiro.domain.course.controller.dto.CourseConfirmPublicTransportResponse;
import com.sodosiro.domain.course.controller.dto.DayConfirm;
import com.sodosiro.domain.route.dto.RouteWaypoint;
import com.sodosiro.domain.route.dto.TransportMode;
import com.sodosiro.domain.route.service.AdjacentRouteResult;
import com.sodosiro.domain.route.service.RouteCalculationService;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** 프론트에서 확정한 일자별 관광지 순서에 이동수단별 구간 데이터를 붙여 응답한다. 저장은 하지 않는다. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseConfirmationService {

    private final TouristSpotRepository touristSpotRepository;
    private final RouteCalculationService routeCalculationService;

    public CourseConfirmCarResponse confirmCar(CourseConfirmCarRequest request) {
        Map<Long, TouristSpot> spotsById = findSpotsByContentId(request.days());
        List<CourseConfirmCarResponse.DayCarRoute> days = request.days().stream()
                .map(dayConfirm -> new CourseConfirmCarResponse.DayCarRoute(
                        dayConfirm.day(), calculateCarLegs(toWaypoints(dayConfirm, spotsById))))
                .toList();
        return new CourseConfirmCarResponse(days);
    }

    public CourseConfirmPublicTransportResponse confirmPublicTransport(CourseConfirmPublicTransportRequest request) {
        Map<Long, TouristSpot> spotsById = findSpotsByContentId(request.days());
        List<CourseConfirmPublicTransportResponse.DayPublicTransportRoute> days = request.days().stream()
                .map(dayConfirm -> new CourseConfirmPublicTransportResponse.DayPublicTransportRoute(
                        dayConfirm.day(), calculatePublicTransportDetails(toWaypoints(dayConfirm, spotsById))))
                .toList();
        return new CourseConfirmPublicTransportResponse(days);
    }

    private List<com.sodosiro.domain.route.dto.RouteLeg> calculateCarLegs(List<RouteWaypoint> waypoints) {
        AdjacentRouteResult result = routeCalculationService.calculateAdjacentRoutes(waypoints, TransportMode.CAR);
        if (result instanceof AdjacentRouteResult.Car car) {
            return car.legs();
        }
        throw new IllegalStateException("예상하지 못한 경로 계산 결과 타입입니다.");
    }

    private List<com.sodosiro.domain.route.odsay.dto.OdsayRouteDetailResponse> calculatePublicTransportDetails(
            List<RouteWaypoint> waypoints) {
        AdjacentRouteResult result = routeCalculationService
                .calculateAdjacentRoutes(waypoints, TransportMode.PUBLIC_TRANSPORT);
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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 관광지가 포함되어 있습니다.");
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