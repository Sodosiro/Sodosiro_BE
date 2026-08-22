package com.sodosiro.domain.notification.controller.dto;

import com.sodosiro.domain.notification.entity.Notification;
import com.sodosiro.domain.notification.entity.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.Map;

public record NotificationResponse(

        @Schema(description = "알림 PK", example = "102")
        Long id,

        @Schema(description = "알림 종류")
        NotificationType type,

        @Schema(description = "알림 제목", example = "내 게시물에 좋아요 2개가 달렸어요")
        String title,

        @Schema(description = "알림 본문. 근처 알림은 여행지명 목록, 디깅 좋아요는 대상 여행지명", example = "영진횟집")
        String body,

        @Schema(description = "알림 종류별 추가 정보. 클릭 시 이동할 대상 식별에 사용한다 "
                + "(DIGGING_POST_LIKE: diggingId·likerUserId, NEARBY_LIKED_SPOTS: nearestContentId·nearbyCount)",
                example = "{\"diggingId\": 3, \"likerUserId\": 100}")
        Map<String, Object> payload,

        @Schema(description = "읽음 여부", example = "false")
        boolean isRead,

        @Schema(description = "생성 일시")
        LocalDateTime createdAt
) {
    public static NotificationResponse from(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getType(),
                notification.getTitle(),
                notification.getBody(),
                notification.getPayload(),
                notification.isRead(),
                notification.getCreatedAt()
        );
    }
}
