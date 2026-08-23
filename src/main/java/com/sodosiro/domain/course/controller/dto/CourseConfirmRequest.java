package com.sodosiro.domain.course.controller.dto;

import jakarta.validation.constraints.NotNull;

public record CourseConfirmRequest(
        @NotNull(message = "코스 ID는 필수입니다.")
        Long courseId
) {
}
