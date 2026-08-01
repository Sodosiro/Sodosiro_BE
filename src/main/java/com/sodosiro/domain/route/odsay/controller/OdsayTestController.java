package com.sodosiro.domain.route.odsay.controller;

import com.sodosiro.domain.route.kakao.dto.RouteWaypoint;
import com.sodosiro.domain.route.odsay.client.OdsayDirectionsClient;
import com.sodosiro.domain.route.odsay.dto.OdsayRouteDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class OdsayTestController {

    private final OdsayDirectionsClient odsayDirectionsClient;

    @GetMapping("/test/odsay/directions")
    public OdsayRouteDetailResponse testDirections(
            @RequestParam BigDecimal startX,
            @RequestParam BigDecimal startY,
            @RequestParam BigDecimal endX,
            @RequestParam BigDecimal endY
    ) {
        RouteWaypoint origin = new RouteWaypoint(1L, startX, startY);
        RouteWaypoint destination = new RouteWaypoint(2L, endX, endY);

        return odsayDirectionsClient.findRouteDetail(origin, destination);
    }
}
