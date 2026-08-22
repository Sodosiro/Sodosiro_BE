package com.sodosiro.domain.notification.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(
        name = "user_device",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_device", columnNames = {"user_id", "device_id"}),
                @UniqueConstraint(name = "uk_user_device_fcm_token", columnNames = "fcm_token")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDevice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "device_id", nullable = false, length = 200)
    private String deviceId;

    @Column(name = "fcm_token", length = 512)
    private String fcmToken;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Platform platform;

    @Column(name = "push_enabled", nullable = false)
    private boolean pushEnabled;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Column(name = "invalidated_at")
    private LocalDateTime invalidatedAt;

    public static UserDevice create(Long userId, String deviceId, String token, Platform platform) {
        UserDevice device = new UserDevice();
        device.userId = userId;
        device.deviceId = deviceId;
        device.fcmToken = token;
        device.platform = platform;
        device.pushEnabled = true;
        device.lastSeenAt = LocalDateTime.now();
        return device;
    }

    public void refresh(String token, Platform platform) {
        fcmToken = token;
        this.platform = platform;
        pushEnabled = true;
        invalidatedAt = null;
        lastSeenAt = LocalDateTime.now();
    }

    public void invalidate() {
        invalidatedAt = LocalDateTime.now();
        pushEnabled = false;
    }

    public void detachToken() {
        fcmToken = null;
        invalidate();
    }

    public boolean filterOutSameUserAndDevice(Long userId, String deviceId){
        return !this.userId.equals(userId) || !this.deviceId.equals(deviceId);
    }
}
