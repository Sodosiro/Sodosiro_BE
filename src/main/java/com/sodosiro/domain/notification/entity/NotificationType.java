package com.sodosiro.domain.notification.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        알림 종류.
        NEARBY_LIKED_SPOTS: 찜한 장소 근처 진입 (payload: courseId, nearbyContentIds(거리순 배열), nearbyCount)
        REVIEW_REQUEST: 리뷰 작성 요청 (payload: courseId, pendingSpotCount)
        DIGGING_POST_LIKE: 내 디깅에 좋아요 (payload: diggingId, likerUserId)
        COURSE_CONFIRM_REMINDER: 여행 시작 D-1 미확정 코스 확정 유도 (payload: courseId, startDate)
        """)
public enum NotificationType {
    NEARBY_LIKED_SPOTS,
    REVIEW_REQUEST,
    DIGGING_POST_LIKE,
    COURSE_CONFIRM_REMINDER
}
