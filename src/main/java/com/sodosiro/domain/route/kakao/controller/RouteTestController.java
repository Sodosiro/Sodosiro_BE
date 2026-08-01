package com.sodosiro.domain.route.kakao.controller;
import com.sodosiro.domain.route.kakao.dto.RouteDirectionsResponse;
import com.sodosiro.domain.route.kakao.dto.RouteWaypoint;
import com.sodosiro.domain.route.kakao.dto.RouteLeg;
import com.sodosiro.domain.route.kakao.service.RouteLegTimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class RouteTestController {

    private final RouteLegTimeService routeLegTimeService;

    @GetMapping("/test/directions")
    public RouteDirectionsResponse testDirections(
            @RequestParam BigDecimal startX,
            @RequestParam BigDecimal startY,
            @RequestParam BigDecimal endX,
            @RequestParam BigDecimal endY
    ) {
        List<RouteWaypoint> waypoints = List.of(
                new RouteWaypoint(1L, startX, startY),
                new RouteWaypoint(2L, endX, endY)
        );
        List<RouteLeg> legs = routeLegTimeService.calculateAdjacentLegTimes(waypoints);

        return RouteDirectionsResponse.from(legs);
    }
}