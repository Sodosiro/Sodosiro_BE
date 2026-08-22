package com.sodosiro.domain.notification.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "notification_preference")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationPreference {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static NotificationPreference create(Long userId, boolean pushEnabled) {
        NotificationPreference preference = new NotificationPreference();
        preference.userId = userId;
        preference.pushEnabled = pushEnabled;
        preference.updatedAt = LocalDateTime.now();
        return preference;
    }

    public void updatePushEnabled(boolean pushEnabled) {
        this.pushEnabled = pushEnabled;
        this.updatedAt = LocalDateTime.now();
    }
}
