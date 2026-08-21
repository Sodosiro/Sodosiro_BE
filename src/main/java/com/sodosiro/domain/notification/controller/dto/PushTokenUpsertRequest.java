package com.sodosiro.domain.notification.controller.dto;

import com.sodosiro.domain.notification.entity.Platform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PushTokenUpsertRequest(

        @Schema(description = "FCM 등록 토큰", example = "elfSyEVcSZqHS-bemxHe...")
        @NotBlank String fcmToken,

        @Schema(description = "기기 플랫폼", example = "ANDROID")
        @NotNull Platform platform
) {
}
