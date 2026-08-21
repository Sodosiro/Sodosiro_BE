package com.sodosiro.domain.route.service;

import com.sodosiro.domain.route.dto.RouteWaypoint;
import com.sodosiro.domain.route.dto.TransportMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** 사용자가 온보딩에서 고른 이동수단에 따라 자차/대중교통 계산을 분기하는 진입점 */
@Service
@RequiredArgsConstructor
public class RouteCalculationService {

    private final RouteLegTimeService routeLegTimeService;
    private final KakaoTransitRouteService kakaoTransitRouteService;

    public AdjacentRouteResult calculateAdjacentRoutes(List<RouteWaypoint> orderedWaypoints, TransportMode mode) {
        return switch (mode) {
            case CAR -> new AdjacentRouteResult.Car(routeLegTimeService.calculateAdjacentLegTimes(orderedWaypoints));
            case PUBLIC_TRANSPORT -> new AdjacentRouteResult.PublicTransport(kakaoTransitRouteService.calculateAdjacentRouteDetails(orderedWaypoints));
        };
    }
}
