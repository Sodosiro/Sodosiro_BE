package com.sodosiro.domain.notification.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record NotificationListResponse(

        @Schema(description = "알림 목록 (최신순)")
        List<NotificationResponse> items,

        @Schema(description = "다음 페이지 요청 시 cursor 로 전달할 값. 마지막 페이지면 null", example = "95")
        String nextCursor,

        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext,

        @Schema(description = "사용자의 전체 안 읽은 알림 수 (커서와 무관)", example = "12")
        long unreadCount
) {
}
