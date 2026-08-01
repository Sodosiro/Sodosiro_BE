package com.sodosiro.domain.route;


import com.sodosiro.domain.route.kakao.dto.RouteLeg;
import com.sodosiro.domain.route.kakao.dto.RouteWaypoint;

public interface RouteSearchClient {
    RouteLeg findRoute(RouteWaypoint origin, RouteWaypoint destination);


}