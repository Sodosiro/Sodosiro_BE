package com.sodosiro.domain.route.kakao.client;

import com.sodosiro.domain.route.kakao.dto.DirectionsLegResult;
import com.sodosiro.domain.route.kakao.dto.KakaoDirectionsResponse;
import com.sodosiro.domain.route.kakao.dto.KakaoRoute;
import com.sodosiro.domain.route.dto.RouteLeg;
import com.sodosiro.domain.route.dto.RouteWaypoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoDirectionsClient {

    private static final int SUCCESS_RESULT_CODE = 0;

    private final RestClient kakaoMobilityRestClient;

    public RouteLeg findRoute(RouteWaypoint origin, RouteWaypoint destination) {
        try {
            KakaoDirectionsResponse response = kakaoMobilityRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v1/directions")
                            .queryParam("origin", toCoordinateParam(origin))
                            .queryParam("destination", toCoordinateParam(destination))
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

    private static RouteLeg toRouteLeg(RouteWaypoint origin, RouteWaypoint destination, DirectionsLegResult result) {
        if (!result.success()) {
            return RouteLeg.failure(origin.id(), destination.id());
        }
        return RouteLeg.success(origin.id(), destination.id(), result.durationSeconds(), result.distanceMeters());
    }

    private static DirectionsLegResult toResult(KakaoDirectionsResponse response) {

        if (response == null || response.routes() == null || response.routes().isEmpty()) {
            return DirectionsLegResult.failure();
        }

        KakaoRoute route = response.routes().get(0);
        if (route.resultCode() != SUCCESS_RESULT_CODE || route.summary() == null) {
            return DirectionsLegResult.failure();
        }

        return DirectionsLegResult.success(route.summary().duration(), route.summary().distance());
    }
}
