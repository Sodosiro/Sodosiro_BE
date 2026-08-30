package com.sodosiro.domain.notification.controller.dto;

import com.sodosiro.domain.notification.service.dto.CourseConfirmReminderBatchResult;

public record CourseConfirmReminderBatchResponse(
        int targetedCourses,
        int createdNotifications,
        int skippedByCooldown
) {
    public static CourseConfirmReminderBatchResponse from(CourseConfirmReminderBatchResult result) {
        return new CourseConfirmReminderBatchResponse(
                result.targetedCourses(),
                result.createdNotifications(),
                result.skippedByCooldown()
        );
    }
}
