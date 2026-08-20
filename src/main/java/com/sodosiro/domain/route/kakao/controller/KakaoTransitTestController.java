package com.sodosiro.domain.route.kakao.controller;

import com.sodosiro.domain.route.dto.RouteWaypoint;
import com.sodosiro.domain.route.kakao.client.KakaoTransitDirectionsClient;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitRouteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequiredArgsConstructor
public class KakaoTransitTestController {

    private final KakaoTransitDirectionsClient kakaoTransitDirectionsClient;

    @GetMapping("/test/kakao/transit")
    public KakaoTransitRouteResult testTransitDirections(
            @RequestParam BigDecimal startX,
            @RequestParam BigDecimal startY,
            @RequestParam BigDecimal endX,
            @RequestParam BigDecimal endY
    ) {
        RouteWaypoint origin = new RouteWaypoint(1L, startX, startY);
        RouteWaypoint destination = new RouteWaypoint(2L, endX, endY);

        return kakaoTransitDirectionsClient.findRouteDetail(origin, destination);
    }
}
