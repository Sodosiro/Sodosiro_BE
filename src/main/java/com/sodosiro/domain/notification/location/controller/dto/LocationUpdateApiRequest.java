package com.sodosiro.domain.notification.location.controller.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record LocationUpdateApiRequest(

        @NotNull(message = "위도는 필수입니다.")
        @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다.")
        @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다.")
        Double latitude,

        @NotNull(message = "경도는 필수입니다.")
        @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다.")
        @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다.")
        Double longitude,

        @NotNull(message = "정확도는 필수입니다.")
        @DecimalMin(value = "0.0", message = "정확도는 0 이상이어야 합니다.")
        Double accuracy,

        @NotNull(message = "occurredAt은 필수입니다.")
        Instant occurredAt
) {
}
