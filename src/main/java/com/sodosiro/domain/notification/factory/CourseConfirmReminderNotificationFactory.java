package com.sodosiro.domain.notification.factory;

import com.sodosiro.domain.notification.command.NotificationCommand;
import com.sodosiro.domain.notification.entity.NotificationType;
import com.sodosiro.domain.notification.trigger.CourseConfirmReminderEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class CourseConfirmReminderNotificationFactory
        implements NotificationFactory<CourseConfirmReminderEvent> {

    @Value("${notification.course-confirm-reminder.cooldown-days:7}")
    private long cooldownDays;

    @Override
    public Class<CourseConfirmReminderEvent> triggerType() {
        return CourseConfirmReminderEvent.class;
    }

    @Override
    public List<NotificationCommand> create(CourseConfirmReminderEvent event) {
        String title = "아직 확정하지 않은 일정이 있어요";
        String body = "내일까지 확정하지 않으면 일정이 삭제돼요.";

        return List.of(new NotificationCommand(
                event.userId(),
                NotificationType.COURSE_CONFIRM_REMINDER,
                title,
                body,
                Map.of("courseId", event.courseId(),
                        "startDate", event.startDate().toString()),
                dedupeKey(event.courseId()),
                LocalDateTime.now().plusDays(cooldownDays)
        ));
    }

    static String dedupeKey(Long courseId) {
        return "COURSE_CONFIRM_REMIND:COURSE:%d".formatted(courseId);
    }
}
