package com.sodosiro.domain.notification.entity;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "notification_delivery_guard",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_notification_delivery_guard",
                columnNames = {"user_id", "type", "dedupe_key"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationDeliveryGuard {
    @EmbeddedId
    private Id id;

    @Column(name = "last_sent_at", nullable = false)
    private LocalDateTime lastSentAt;

    @Column(name = "cooldown_until", nullable = false)
    private LocalDateTime cooldownUntil;

    @Embeddable
    @EqualsAndHashCode
    public static class Id implements Serializable {
        @Column(name = "user_id")
        private Long userId;

        @Enumerated(EnumType.STRING)
        @Column(name = "type", length = 40)
        private NotificationType type;

        @Column(name = "dedupe_key", length = 200)
        private String dedupeKey;
    }
}
