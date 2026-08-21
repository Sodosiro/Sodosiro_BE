package com.sodosiro.domain.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(name = "notification", indexes = {
        @Index(name = "idx_notification_user_created", columnList = "user_id, created_at DESC"),
        @Index(name = "idx_notification_user_unread", columnList = "user_id, is_read")
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String body;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    @Column(name = "is_read", nullable = false)
    private boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    private Notification(Long userId, NotificationType type, String title, String body, Map<String, Object> payload) {
        this.userId = userId;
        this.type = type;
        this.title = title;
        this.body = body;
        this.payload = payload;
        this.createdAt = LocalDateTime.now();
    }

    public static Notification create(Long userId, NotificationType type, String title, String body, Map<String, Object> payload) {
        return new Notification(userId, type, title, body, payload);
    }
    public void markRead() {
        if (isRead) {
            return;
        }
        isRead = true;
        readAt = LocalDateTime.now();
        updatedAt = readAt;
    }
}
