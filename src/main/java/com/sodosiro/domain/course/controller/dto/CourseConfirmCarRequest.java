package com.sodosiro.domain.course.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CourseConfirmCarRequest(
        @NotEmpty(message = "일자별 코스는 비어 있을 수 없습니다.")
        @Valid
        List<DayConfirm> days
) {
}
