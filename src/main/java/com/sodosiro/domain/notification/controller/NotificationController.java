package com.sodosiro.domain.notification.controller;

import com.sodosiro.domain.notification.controller.dto.*;
import com.sodosiro.domain.notification.controller.specification.NotificationSpecification;
import com.sodosiro.domain.notification.service.NotificationPreferenceService;
import com.sodosiro.domain.notification.service.NotificationQueryService;
import com.sodosiro.domain.notification.service.UserDeviceService;
import com.sodosiro.global.resolver.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController implements NotificationSpecification {
    private final NotificationQueryService notificationQueryService;
    private final UserDeviceService userDeviceService;
    private final NotificationPreferenceService notificationPreferenceService;

    @GetMapping("/notifications")
    public ResponseEntity<NotificationListResponse> list(
            @LoginUser Long userId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(notificationQueryService.list(userId, cursor, size));
    }

    @PatchMapping("/notifications/{notificationId}/read")
    public ResponseEntity<Void> read(@LoginUser Long userId, @PathVariable Long notificationId) {
        notificationQueryService.markRead(userId, notificationId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/notifications/read-all")
    public ResponseEntity<Void> readAll(@LoginUser Long userId) {
        notificationQueryService.markAllRead(userId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/devices/{deviceId}/push-token")
    public ResponseEntity<Void> upsertToken(
            @LoginUser Long userId,
            @PathVariable String deviceId,
            @Valid @RequestBody PushTokenUpsertRequest request) {
        userDeviceService.upsert(userId, deviceId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/devices/{deviceId}")
    public ResponseEntity<Void> deleteDevice(@LoginUser Long userId, @PathVariable String deviceId) {
        userDeviceService.deactivate(userId, deviceId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/notifications/preferences")
    public ResponseEntity<NotificationPreferenceResponse> getPreference(@LoginUser Long userId) {
        return ResponseEntity.ok(new NotificationPreferenceResponse(notificationPreferenceService.isPushEnabled(userId)));
    }

    @PatchMapping("/notifications/preferences")
    public ResponseEntity<NotificationPreferenceResponse> updatePreference(
            @LoginUser Long userId, @Valid @RequestBody NotificationPreferenceRequest request) {
        boolean updated = notificationPreferenceService.updatePushEnabled(userId, request.pushEnabled());
        return ResponseEntity.ok(new NotificationPreferenceResponse(updated));
    }
}
