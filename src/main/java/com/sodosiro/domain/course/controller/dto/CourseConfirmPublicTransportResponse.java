package com.sodosiro.domain.course.controller.dto;

import com.sodosiro.domain.route.kakao.dto.KakaoTransitRouteResult;
import java.util.List;

public record CourseConfirmPublicTransportResponse(List<DayPublicTransportRoute> days) {

    public record DayPublicTransportRoute(int day, List<KakaoTransitRouteResult> details) {
    }
}
