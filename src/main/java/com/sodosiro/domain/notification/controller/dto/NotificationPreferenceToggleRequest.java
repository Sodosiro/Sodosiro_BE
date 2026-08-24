package com.sodosiro.domain.notification.controller.dto;

import com.sodosiro.domain.notification.entity.NotificationPreferenceType;
import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceToggleRequest(
        @NotNull(message = "type은 필수입니다.")
        NotificationPreferenceType type,

        @NotNull(message = "enabled는 필수입니다.")
        Boolean enabled
) {
}
