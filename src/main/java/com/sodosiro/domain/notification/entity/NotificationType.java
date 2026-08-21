package com.sodosiro.domain.notification.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        알림 종류.
        NEARBY_LIKED_SPOTS: 찜한 장소 근처 진입 (payload: nearestContentId, nearbyCount)
        REVIEW_REQUEST: 리뷰 작성 요청
        DIGGING_POST_LIKE: 내 디깅에 좋아요 (payload: diggingId, likerUserId)
        """)
public enum NotificationType {
    NEARBY_LIKED_SPOTS,
    REVIEW_REQUEST,
    DIGGING_POST_LIKE
}
