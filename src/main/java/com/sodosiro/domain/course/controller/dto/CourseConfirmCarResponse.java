package com.sodosiro.domain.course.controller.dto;

import com.sodosiro.domain.route.dto.RouteLeg;
import java.util.List;

public record CourseConfirmCarResponse(List<DayCarRoute> days) {

    public record DayCarRoute(int day, List<RouteLeg> legs) {
    }
}
