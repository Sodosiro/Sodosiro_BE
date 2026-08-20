package com.sodosiro.domain.route.kakao.client;

import com.sodosiro.domain.route.kakao.dto.DirectionsLegResult;
import com.sodosiro.domain.route.kakao.dto.KakaoDirectionsResponse;
import com.sodosiro.domain.route.kakao.dto.KakaoFare;
import com.sodosiro.domain.route.kakao.dto.KakaoRoad;
import com.sodosiro.domain.route.kakao.dto.KakaoRoute;
import com.sodosiro.domain.route.kakao.dto.KakaoSection;
import com.sodosiro.domain.route.dto.RouteCoordinate;
import com.sodosiro.domain.route.dto.RouteLeg;
import com.sodosiro.domain.route.dto.RouteWaypoint;
import com.sodosiro.domain.route.service.CarFuelCostEstimator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoDirectionsClient {

    private static final int SUCCESS_RESULT_CODE = 0;

    private final RestClient kakaoMobilityRestClient;
    private final CarFuelCostEstimator carFuelCostEstimator;

    public RouteLeg findRoute(RouteWaypoint origin, RouteWaypoint destination) {
        try {
            KakaoDirectionsResponse response = kakaoMobilityRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/directions")
                            .queryParam("origin", toCoordinateParam(origin))
                            .queryParam("destination", toCoordinateParam(destination))
                            .queryParam("road_details", true)
                            .build())
                    .retrieve()
                    .body(KakaoDirectionsResponse.class);

            return toRouteLeg(origin, destination, toResult(response));
        } catch (RestClientException exception) {
            log.warn("카카오 길찾기 API 호출 실패: origin={}, destination={},reason={}", origin.id(), destination.id(), exception.getMessage());
            return RouteLeg.failure(origin.id(), destination.id());
        }
    }

    private static String toCoordinateParam(RouteWaypoint waypoint) {
        return waypoint.x().toPlainString() + "," + waypoint.y().toPlainString();
    }

    private RouteLeg toRouteLeg(RouteWaypoint origin, RouteWaypoint destination, DirectionsLegResult result) {
        if (!result.success()) {
            return RouteLeg.failure(origin.id(), destination.id());
        }
        long estimatedFuelCost = carFuelCostEstimator.estimate(result.distanceMeters());
        return RouteLeg.success(
                origin.id(), destination.id(), result.durationSeconds(), result.distanceMeters(),
                result.tollFare(), estimatedFuelCost, result.path());
    }

    private static DirectionsLegResult toResult(KakaoDirectionsResponse response) {

        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            return DirectionsLegResult.failure();
        }

        KakaoRoute route = response.routes().get(0);
        if (route.resultCode() != SUCCESS_RESULT_CODE || route.summary() == null) {
            return DirectionsLegResult.failure();
        }

        return DirectionsLegResult.success(
                route.summary().duration(), route.summary().distance(), tollFare(route.summary().fare()), toPath(route));
    }

    private static Long tollFare(KakaoFare fare) {
        return fare == null || fare.toll() == null ? null : fare.toll().longValue();
    }

    /** road_details=true로 받은 sections[].roads[].vertexes를 이어붙여 지도에 그릴 경로선 좌표를 만든다 */
    private static List<RouteCoordinate> toPath(KakaoRoute route) {
        if (route.sections() == null) {
            return List.of();
        }
        return route.sections().stream()
                .filter(Objects::nonNull)
                .flatMap(KakaoDirectionsClient::roadsOf)
                .flatMap(road -> toCoordinates(road.vertexes()).stream())
                .toList();
    }

    private static Stream<KakaoRoad> roadsOf(KakaoSection section) {
        return section.roads() == null ? Stream.empty() : section.roads().stream().filter(Objects::nonNull);
    }

    private static List<RouteCoordinate> toCoordinates(List<Double> vertexes) {
        if (vertexes == null) {
            return List.of();
        }
        List<RouteCoordinate> coordinates = new ArrayList<>();
        for (int i = 0; i + 1 < vertexes.size(); i += 2) {
            coordinates.add(new RouteCoordinate(BigDecimal.valueOf(vertexes.get(i)), BigDecimal.valueOf(vertexes.get(i + 1))));
        }
        return coordinates;
    }
}
