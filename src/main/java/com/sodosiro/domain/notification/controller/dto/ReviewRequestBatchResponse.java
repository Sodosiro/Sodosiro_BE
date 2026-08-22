package com.sodosiro.domain.notification.controller.dto;

import com.sodosiro.domain.notification.service.dto.ReviewRequestBatchResult;

public record ReviewRequestBatchResponse(
        int targetedCourses,
        int createdNotifications,
        int skippedByCooldown
) {
    public static ReviewRequestBatchResponse from(ReviewRequestBatchResult result) {
        return new ReviewRequestBatchResponse(
                result.targetedCourses(),
                result.createdNotifications(),
                result.skippedByCooldown()
        );
    }
}
