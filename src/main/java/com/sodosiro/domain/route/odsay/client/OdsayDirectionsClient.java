package com.sodosiro.domain.route.odsay.client;

import com.sodosiro.domain.route.RouteSearchClient;
import com.sodosiro.domain.route.kakao.dto.RouteLeg;
import com.sodosiro.domain.route.kakao.dto.RouteWaypoint;
import com.sodosiro.domain.route.odsay.dto.OdsayLegResult;
import com.sodosiro.domain.route.odsay.dto.OdsayPathResponse;
import com.sodosiro.domain.route.odsay.dto.OdsayRouteDetailResponse;
import com.sodosiro.domain.route.odsay.dto.OdsaySegmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class OdsayDirectionsClient implements RouteSearchClient {

    private final RestClient odsayRestClient;

    @Value("${odsay.api.key}")
    private String apiKey;

    public RouteLeg findRoute(RouteWaypoint origin, RouteWaypoint destination) {
        try {
            OdsayPathResponse response = callOdsayApi(origin, destination);
            return toRouteLeg(origin, destination, toResult(response));
        } catch (RestClientException exception) {
            log.warn("ODsay 대중교통 길찾기 API 호출 실패: origin={}, destination={}", origin.id(), destination.id(), exception);
            return RouteLeg.failure(origin.id(), destination.id());
        }
    }

    /** 도보/버스/지하철 구간별 상세 정보(수동 테스트용) */
    public OdsayRouteDetailResponse findRouteDetail(RouteWaypoint origin, RouteWaypoint destination) {
        try {
            OdsayPathResponse response = callOdsayApi(origin, destination);
            return toDetailResponse(response);
        } catch (RestClientException exception) {
            log.warn("ODsay 대중교통 길찾기 API 호출 실패: origin={}, destination={}", origin.id(), destination.id(), exception);
            return OdsayRouteDetailResponse.failure();
        }
    }

    private OdsayPathResponse callOdsayApi(RouteWaypoint origin, RouteWaypoint destination) {
        return odsayRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/api/searchPubTransPathT")
                        .queryParam("apiKey", apiKey)
                        .queryParam("SX", origin.x())
                        .queryParam("SY", origin.y())
                        .queryParam("EX", destination.x())
                        .queryParam("EY", destination.y())
                        .queryParam("output", "json")
                        .build())
                .retrieve()
                .body(OdsayPathResponse.class);
    }

    private static RouteLeg toRouteLeg(RouteWaypoint origin, RouteWaypoint destination, OdsayLegResult result) {
        if (!result.success()) {
            return RouteLeg.failure(origin.id(), destination.id());
        }
        return RouteLeg.success(origin.id(), destination.id(), result.durationSeconds(), result.distanceMeters());
    }

    private static OdsayLegResult toResult(OdsayPathResponse response) {
        OdsayPathResponse.OdsayPath bestPath = bestPath(response);
        OdsayPathResponse.OdsayInfo info = bestPath == null ? null : bestPath.info();
        if (info == null || info.totalTime() == null || info.totalDistance() == null || info.payment() == null) {
            return OdsayLegResult.failure();
        }

        return OdsayLegResult.success(info.totalTime(), info.totalDistance(), info.payment());
    }

    private static OdsayRouteDetailResponse toDetailResponse(OdsayPathResponse response) {
        OdsayPathResponse.OdsayPath bestPath = bestPath(response);
        OdsayPathResponse.OdsayInfo info = bestPath == null ? null : bestPath.info();
        if (info == null || info.totalTime() == null || info.totalDistance() == null || info.payment() == null) {
            return OdsayRouteDetailResponse.failure();
        }

        List<OdsaySegmentResponse> segments = bestPath.subPaths() == null
                ? List.of()
                : bestPath.subPaths().stream().map(OdsayDirectionsClient::toSegment).toList();

        return OdsayRouteDetailResponse.success(info.totalTime(), info.totalDistance(), info.payment(), segments);
    }

    private static OdsaySegmentResponse toSegment(OdsayPathResponse.OdsaySubPath subPath) {
        List<String> laneNames = subPath.lanes() == null
                ? List.of()
                : subPath.lanes().stream()
                        .map(OdsayPathResponse.OdsayLane::name)
                        .filter(Objects::nonNull)
                        .toList();

        return new OdsaySegmentResponse(toTrafficTypeName(subPath.trafficType()), subPath.sectionTime(), subPath.distance(), laneNames);
    }

    /** ODsay trafficType: 1=지하철, 2=버스, 3=도보 */
    private static String toTrafficTypeName(Integer trafficType) {
        if (trafficType == null) {
            return "알수없음";
        }
        return switch (trafficType) {
            case 1 -> "지하철";
            case 2 -> "버스";
            case 3 -> "도보";
            default -> "기타";
        };
    }

    /** 보통 최적 경로(첫 번째 경로)를 사용 */
    private static OdsayPathResponse.OdsayPath bestPath(OdsayPathResponse response) {
        if (response == null || response.result() == null || response.result().paths() == null || response.result().paths().isEmpty()) {
            return null;
        }
        return response.result().paths().get(0);
    }
}