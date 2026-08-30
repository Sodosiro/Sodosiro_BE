package com.sodosiro.domain.notification.service.dto;

public record CourseConfirmReminderBatchResult(
        int targetedCourses,
        int createdNotifications,
        int skippedByCooldown
) {
}
