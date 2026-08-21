package com.sodosiro.domain.notification.factory;

import com.sodosiro.domain.notification.command.NotificationCommand;
import com.sodosiro.domain.notification.entity.NotificationType;
import com.sodosiro.domain.notification.trigger.ReviewRequestTargetEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 리뷰 작성 유도 알림. 쿨다운 적용 중복 발송 x
 */
@Component
public class ReviewRequestNotificationFactory implements NotificationFactory<ReviewRequestTargetEvent> {

    @Value("${notification.review-request.cooldown-days:30}")
    private long cooldownDays;

    @Override
    public Class<ReviewRequestTargetEvent> triggerType() {
        return ReviewRequestTargetEvent.class;
    }

    @Override
    public List<NotificationCommand> create(ReviewRequestTargetEvent event) {
        String title = "%s 여행은 어떠셨나요?".formatted(event.courseName());
        String body = "다녀오신 곳의 리뷰를 남겨주세요. 다른 여행자에게 큰 도움이 돼요!";

        return List.of(new NotificationCommand(
                event.userId(),
                NotificationType.REVIEW_REQUEST,
                title,
                body,
                Map.of("courseId", event.courseId(),
                        "pendingSpotCount", event.pendingSpotCount()),
                dedupeKey(event.courseId()),
                LocalDateTime.now().plusDays(cooldownDays)
        ));
    }

    static String dedupeKey(Long courseId) {
        return "REVIEW_REQ:COURSE:%d".formatted(courseId);
    }
}
