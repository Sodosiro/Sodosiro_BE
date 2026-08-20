package com.sodosiro.domain.route.kakao.client;

import com.sodosiro.domain.route.kakao.dto.KakaoTransitStepResult;
import com.sodosiro.domain.route.kakao.dto.KakaoWalkResponse;
import com.sodosiro.domain.route.kakao.dto.KakaoWalkStep;
import com.sodosiro.domain.route.kakao.dto.KakaoWalkStepProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** publictraffic API가 대중교통 구간만 주기 때문에, 정류장까지/정류장에서부터의 도보 구간을 채우기 위해 별도로 호출한다 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoWalkDirectionsClient {

    private static final String SUCCESS_STATUS = "OK";

    private final RestClient kakaoMapRestClient;

    public List<KakaoTransitStepResult> findWalkSteps(
            BigDecimal startX, BigDecimal startY, BigDecimal endX, BigDecimal endY) {
        try {
            KakaoWalkResponse response = kakaoMapRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/v2/routing/walk")
                            .queryParam("start_x", startX.toPlainString())
                            .queryParam("start_y", startY.toPlainString())
                            .queryParam("end_x", endX.toPlainString())
                            .queryParam("end_y", endY.toPlainString())
                            .build())
                    .retrieve()
                    .body(KakaoWalkResponse.class);

            return toSteps(response);
        } catch (RestClientException exception) {
            log.warn("카카오 도보 길찾기 API 호출 실패: start=({}, {}), end=({}, {})",
                    startX, startY, endX, endY, exception);
            return List.of();
        }
    }

    private static List<KakaoTransitStepResult> toSteps(KakaoWalkResponse response) {
        if (response == null || !SUCCESS_STATUS.equals(response.status())
                || response.route() == null || response.route().legs() == null) {
            return List.of();
        }

        return response.route().legs().stream()
                .filter(Objects::nonNull)
                .flatMap(leg -> leg.steps() == null ? Stream.<KakaoWalkStep>empty() : leg.steps().stream())
                .filter(Objects::nonNull)
                .map(KakaoWalkDirectionsClient::toStepResult)
                .toList();
    }

    private static KakaoTransitStepResult toStepResult(KakaoWalkStep step) {
        KakaoWalkStepProperties properties = step.properties();

        return new KakaoTransitStepResult(
                "WALKING",
                properties == null ? null : properties.guidance(),
                properties == null ? null : properties.distance(),
                properties == null ? null : properties.time(),
                List.of(),
                List.of(),
                KakaoTransitDirectionsClient.toCoordinates(step.path())
        );
    }
}
