package com.sodosiro.domain.course.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CourseRecommendRequest(
        @NotNull(message = "여행 시작일은 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "여행 종료일은 필수입니다.")
        LocalDate endDate,

        @Size(max = 2, message = "여행스타일은 최대 2개까지 선택할 수 있습니다.")
        List<TravelStyle> travelStyles,

        Long mustVisitContentId,

        String aiMessage
) {
    public List<TravelStyle> travelStylesOrEmpty() {
        return travelStyles == null ? List.of() : travelStyles;
    }
}
