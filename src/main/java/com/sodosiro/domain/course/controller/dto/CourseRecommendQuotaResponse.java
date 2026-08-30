package com.sodosiro.domain.course.controller.dto;

public record CourseRecommendQuotaResponse(
        int limit,
        int used,
        int remaining
) {
}
