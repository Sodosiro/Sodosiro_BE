package com.sodosiro.domain.route;


import com.sodosiro.domain.route.dto.RouteLeg;
import com.sodosiro.domain.route.dto.RouteWaypoint;

public interface RouteSearchClient {
    RouteLeg findRoute(RouteWaypoint origin, RouteWaypoint destination);


}