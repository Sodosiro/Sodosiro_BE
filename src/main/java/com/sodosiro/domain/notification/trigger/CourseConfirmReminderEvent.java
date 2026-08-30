package com.sodosiro.domain.notification.trigger;

import java.time.LocalDate;

public record CourseConfirmReminderEvent(
        Long userId,
        Long courseId,
        LocalDate startDate     // 여행 시작일 (알림 시점 기준 내일)
) implements NotificationTrigger {
}
