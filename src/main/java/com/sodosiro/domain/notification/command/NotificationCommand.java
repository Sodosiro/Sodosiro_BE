package com.sodosiro.domain.notification.command;

import com.sodosiro.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;
import java.util.Map;

public record NotificationCommand(
        Long recipientUserId,
        NotificationType type,
        String title,
        String body,
        Map<String, Object> payload,
        String dedupeKey,
        LocalDateTime cooldownUntil
) {
}
