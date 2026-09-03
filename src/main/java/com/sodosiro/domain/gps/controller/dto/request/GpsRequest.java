package com.sodosiro.domain.gps.controller.dto.request;

import jakarta.validation.constraints.NotNull;

/** 위치 인증은 프론트에서 완료 후 호출한다. 서버는 별도 좌표 검증 없이 인증 완료로 처리한다. */
public record GpsRequest(
        @NotNull(message = "courseId는 필수입니다.")
        Long courseId,

        @NotNull(message = "contentId는 필수입니다.")
        Long contentId,

        @NotNull(message = "day는 필수입니다.")
        Integer day
) {
}
