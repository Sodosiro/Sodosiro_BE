package com.sodosiro.domain.notification.service.dto;

import com.sodosiro.domain.notification.trigger.CourseConfirmReminderEvent;
import java.time.LocalDate;

public record CourseConfirmReminderTarget(
        Long userId,
        Long courseId,
        LocalDate startDate
) {
    public CourseConfirmReminderEvent toEvent() {
        return new CourseConfirmReminderEvent(userId, courseId, startDate);
    }
}
