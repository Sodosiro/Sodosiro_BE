package com.sodosiro.domain.notification.controller.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceRequest(
        @NotNull(message = "pushEnabled는 필수입니다.")
        Boolean pushEnabled
) {
}
