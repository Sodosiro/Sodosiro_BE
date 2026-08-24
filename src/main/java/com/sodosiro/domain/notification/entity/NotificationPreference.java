package com.sodosiro.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 유저별 알림 수신 설정. 앱 내 알림(Notification) 저장 여부와는 무관하다 — 타입별 FCM 푸시 전송만 제어한다. */
@Entity
@Getter
@Table(name = "notification_preference")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "all_enabled", nullable = false)
    private boolean allEnabled;

    @Column(name = "nearby_liked_spots_enabled", nullable = false)
    private boolean nearbyLikedSpotsEnabled;

    @Column(name = "review_request_enabled", nullable = false)
    private boolean reviewRequestEnabled;

    @Column(name = "digging_post_like_enabled", nullable = false)
    private boolean diggingPostLikeEnabled;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private NotificationPreference(Long userId) {
        this.userId = userId;
        this.allEnabled = true;
        this.nearbyLikedSpotsEnabled = true;
        this.reviewRequestEnabled = true;
        this.diggingPostLikeEnabled = true;
        this.updatedAt = LocalDateTime.now();
    }

    public static NotificationPreference createDefault(Long userId) {
        return new NotificationPreference(userId);
    }

    public void updateEnabled(NotificationPreferenceType type, boolean enabled) {
        switch (type) {
            case ALL -> this.allEnabled = enabled;
            case NEARBY_LIKED_SPOTS -> this.nearbyLikedSpotsEnabled = enabled;
            case REVIEW_REQUEST -> this.reviewRequestEnabled = enabled;
            case DIGGING_POST_LIKE -> this.diggingPostLikeEnabled = enabled;
        }
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isPushEnabled(NotificationType type) {
        return allEnabled && isTypeEnabled(type);
    }

    private boolean isTypeEnabled(NotificationType type) {
        return switch (type) {
            case NEARBY_LIKED_SPOTS -> nearbyLikedSpotsEnabled;
            case REVIEW_REQUEST -> reviewRequestEnabled;
            case DIGGING_POST_LIKE -> diggingPostLikeEnabled;
        };
    }
}
