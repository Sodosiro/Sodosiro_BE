package com.sodosiro.domain.notification.trigger;

public record DiggingLikedEvent(
        Long diggingAuthorId,
        Long likerUserId,
        Long diggingId,
        String spotTitle,
        int likeCount
) implements NotificationTrigger {
}
