package com.sodosiro.domain.course.controller.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** 프론트에서 장소/순서 변경을 마친 뒤 확정한 일자별 관광지 순서. */
public record DayConfirm(
        int day,

        @NotEmpty(message = "관광지 목록은 비어 있을 수 없습니다.")
        List<Long> contentIds
) {
}
