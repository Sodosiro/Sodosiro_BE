package com.sodosiro.domain.course.controller.dto;

import com.sodosiro.domain.route.dto.TransportMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CourseRecommendResponse(Long courseId, String title, TransportMode transportMode, List<DayCourse> days) {

    public record DayCourse(int day, LocalDate date, List<RecommendedSpot> spots) {
    }

    public record RecommendedSpot(
            Long contentId,
            String title,
            String firstImage,
            BigDecimal mapX,
            BigDecimal mapY,
            Integer category,
            boolean mustVisit
    ) {
    }
}
