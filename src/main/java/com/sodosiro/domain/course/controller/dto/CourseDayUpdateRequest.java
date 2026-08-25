package com.sodosiro.domain.course.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CourseDayUpdateRequest(
        @Size(max = 10, message = "코스 제목은 10자 이하여야 합니다.")
        String title,

        @NotEmpty(message = "일자별 코스는 비어 있을 수 없습니다.")
        @Valid
        List<DayConfirm> days
) {
}
