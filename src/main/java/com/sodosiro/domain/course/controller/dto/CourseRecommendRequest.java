package com.sodosiro.domain.course.controller.dto;

import com.sodosiro.domain.course.constants.TravelStyle;
import com.sodosiro.domain.route.dto.TransportMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record CourseRecommendRequest(
        @NotBlank(message = "지역(시/군)은 필수입니다.")
        String sigunguCode,

        @NotBlank(message = "코스 제목은 필수입니다.")
        @Size(max = 10, message = "코스 제목은 최대 10자까지 입력할 수 있습니다.")
        String title,

        @NotNull(message = "여행 시작일은 필수입니다.")
        LocalDate startDate,

        @NotNull(message = "여행 종료일은 필수입니다.")
        LocalDate endDate,

        @NotNull(message = "이동수단은 필수입니다.")
        TransportMode transportMode,

        @Size(max = 2, message = "여행스타일은 최대 2개까지 선택할 수 있습니다.")
        List<TravelStyle> travelStyles,

        Long mustVisitContentId,

        @Size(max = 20, message = "AI 메시지는 최대 20자까지 입력할 수 있습니다.")
        String aiMessage
) {
    public List<TravelStyle> travelStylesOrEmpty() {
        return travelStyles == null ? List.of() : travelStyles;
    }
}
