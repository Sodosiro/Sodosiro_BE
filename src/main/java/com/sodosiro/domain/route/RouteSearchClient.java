package com.sodosiro.domain.route;


import com.sodosiro.domain.route.dto.RouteLeg;
import com.sodosiro.domain.route.dto.RouteWaypoint;
import com.sodosiro.domain.route.dto.TransportMode;

public interface RouteSearchClient {
    RouteLeg findRoute(RouteWaypoint origin, RouteWaypoint destination);

    TransportMode supports();
}
