package com.sodosiro.domain.gps.controller.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** GPS 원본 좌표는 거리 검증에만 사용하며 저장하지 않는다. */
public record GpsRequest(
        @NotNull(message = "courseId는 필수입니다.")
        Long courseId,

        @NotNull(message = "contentId는 필수입니다.")
        Long contentId,

        @NotNull(message = "day는 필수입니다.")
        Integer day,

        @NotNull(message = "위도는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        BigDecimal latitude,

        @NotNull(message = "경도는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        BigDecimal longitude
) {
}
