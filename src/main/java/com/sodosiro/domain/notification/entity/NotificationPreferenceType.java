package com.sodosiro.domain.notification.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = """
        알림 수신 설정 종류.
        ALL: 전체 알림. 꺼지면 나머지 설정과 무관하게 모든 FCM 푸시가 전송되지 않는다.
        NEARBY_LIKED_SPOTS: 여행 알림 (찜한 장소 근처 진입)
        REVIEW_REQUEST: 여행 리뷰 리마인드
        DIGGING_POST_LIKE: 디깅 좋아요 알림
        """)
public enum NotificationPreferenceType {
    ALL,
    NEARBY_LIKED_SPOTS,
    REVIEW_REQUEST,
    DIGGING_POST_LIKE
}
