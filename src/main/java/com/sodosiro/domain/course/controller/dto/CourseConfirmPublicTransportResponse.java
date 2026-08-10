package com.sodosiro.domain.course.controller.dto;

import com.sodosiro.domain.route.odsay.dto.OdsayRouteDetailResponse;
import java.util.List;

public record CourseConfirmPublicTransportResponse(List<DayPublicTransportRoute> days) {

    public record DayPublicTransportRoute(int day, List<OdsayRouteDetailResponse> details) {
    }
}
