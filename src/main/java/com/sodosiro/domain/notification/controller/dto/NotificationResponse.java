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
                + "(NEARBY_LIKED_SPOTS: courseId·nearbyContentIds(거리순 배열)·nearbyCount, "
                + "REVIEW_REQUEST: courseId·pendingSpotCount, "
                + "DIGGING_POST_LIKE: diggingId·likerUserId, "
                + "COURSE_CONFIRM_REMINDER: courseId·startDate). "
                + "타입별 실제 예시는 GET /v1/notifications 응답 예시를 참고한다.",
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
