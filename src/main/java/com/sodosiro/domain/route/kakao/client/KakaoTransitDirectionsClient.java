package com.sodosiro.domain.route.kakao.client;

import com.sodosiro.domain.route.dto.RouteCoordinate;
import com.sodosiro.domain.route.dto.RouteWaypoint;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitFare;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitPath;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitResponse;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitRoute;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitRouteResult;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitStep;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitStepProperties;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitStepResult;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitStop;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitVehicle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoTransitDirectionsClient {

    private static final String SUCCESS_STATUS = "OK";

    private final RestClient kakaoMapRestClient;
    private final KakaoApiThrottler kakaoMapApiThrottler;

    /** 대중교통은 항상 도보/버스/지하철 구간별 상세 정보와 좌표까지 보여준다 */
    public KakaoTransitRouteResult findRouteDetail(RouteWaypoint origin, RouteWaypoint destination) {
        try {
            String context = "transit from=%d to=%d".formatted(origin.id(), destination.id());
            KakaoTransitResponse response = kakaoMapApiThrottler.execute(() -> kakaoMapRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/routing/publictraffic")
                            .queryParam("start_x", origin.x().toPlainString())
                            .queryParam("start_y", origin.y().toPlainString())
                            .queryParam("end_x", destination.x().toPlainString())
                            .queryParam("end_y", destination.y().toPlainString())
                            .build())
                    .retrieve()
                    .body(KakaoTransitResponse.class), context);

            return toRouteResult(response, origin.id(), destination.id());
        } catch (RestClientException exception) {
            log.warn("카카오 대중교통 길찾기 API 호출 실패: origin={}, destination={}", origin.id(), destination.id(), exception);
            return KakaoTransitRouteResult.failure(origin.id(), destination.id());
        }
    }

    private static KakaoTransitRouteResult toRouteResult(KakaoTransitResponse response, Long fromId, Long toId) {
        KakaoTransitRoute route = bestRoute(response);
        if (route == null || route.properties() == null) {
            return KakaoTransitRouteResult.failure(fromId, toId);
        }

        List<KakaoTransitStepResult> steps = route.steps() == null
                ? List.of()
                : route.steps().stream().filter(Objects::nonNull).map(KakaoTransitDirectionsClient::toStepResult).toList();

        return KakaoTransitRouteResult.success(
                fromId,
                toId,
                route.properties().type(),
                route.properties().totalTime(),
                route.properties().totalDistance(),
                route.properties().transfers(),
                fareValue(route.properties().fare()),
                steps
        );
    }

    private static KakaoTransitStepResult toStepResult(KakaoTransitStep step) {
        KakaoTransitStepProperties properties = step.properties();

        return new KakaoTransitStepResult(
                properties == null ? null : properties.type(),
                properties == null ? null : properties.guidance(),
                properties == null ? null : properties.distance(),
                properties == null ? null : properties.time(),
                stopNames(properties),
                vehicleNames(properties),
                toCoordinates(step.path())
        );
    }

    private static List<String> stopNames(KakaoTransitStepProperties properties) {
        if (properties == null || properties.stops() == null) {
            return List.of();
        }
        return properties.stops().stream().filter(Objects::nonNull).map(KakaoTransitStop::name).toList();
    }

    private static List<String> vehicleNames(KakaoTransitStepProperties properties) {
        if (properties == null || properties.vehicles() == null) {
            return List.of();
        }
        return properties.vehicles().stream().filter(Objects::nonNull).map(KakaoTransitVehicle::name).toList();
    }

    static List<RouteCoordinate> toCoordinates(KakaoTransitPath path) {
        if (path == null || path.points() == null) {
            return List.of();
        }
        return path.points().stream()
                .filter(point -> point != null && point.size() >= 2)
                .map(point -> new RouteCoordinate(BigDecimal.valueOf(point.get(0)), BigDecimal.valueOf(point.get(1))))
                .toList();
    }

    private static Integer fareValue(KakaoTransitFare fare) {
        return fare == null ? null : fare.value();
    }

    /** 보통 최적 경로(첫 번째 경로)를 사용 */
    private static KakaoTransitRoute bestRoute(KakaoTransitResponse response) {
        if (response == null || !SUCCESS_STATUS.equals(response.status())
                || response.routes() == null || response.routes().isEmpty()) {
            return null;
        }
        return response.routes().get(0);
    }
}
