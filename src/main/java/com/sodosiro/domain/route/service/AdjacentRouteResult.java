package com.sodosiro.domain.route.service;

import com.sodosiro.domain.route.dto.RouteLeg;
import com.sodosiro.domain.route.kakao.dto.KakaoTransitRouteResult;

import java.util.List;

/** 자차/대중교통은 응답 형태가 근본적으로 달라 하나의 DTO로 억지로 묶지 않고 타입으로 분리한다 */
public sealed interface AdjacentRouteResult {

    record Car(List<RouteLeg> legs) implements AdjacentRouteResult {}

    record PublicTransport(List<KakaoTransitRouteResult> details) implements AdjacentRouteResult {}
}
