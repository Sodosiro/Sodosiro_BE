package com.sodosiro.domain.notification.factory;

import com.sodosiro.domain.notification.command.NotificationCommand;
import com.sodosiro.domain.notification.entity.NotificationType;
import com.sodosiro.domain.notification.trigger.DiggingLikedEvent;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * 디깅 좋아요 알림.
 * 좋아요는 (digging_id, user_id) 유니크로 유저당 1회만 눌리는 저빈도 이벤트이므로 쿨다운 가드를 두지 않는다.
 */
@Component
public class DiggingLikeNotificationFactory implements NotificationFactory<DiggingLikedEvent> {

    @Override
    public Class<DiggingLikedEvent> triggerType() {
        return DiggingLikedEvent.class;
    }

    @Override
    public List<NotificationCommand> create(DiggingLikedEvent event) {
        // 좋아요 1개면 개수를 생략하고, 2개 이상이면 누적 개수를 함께 보여준다.
        String title = event.likeCount() <= 1
                ? "내 게시물에 좋아요가 달렸어요"
                : "내 게시물에 좋아요 %d개가 달렸어요".formatted(event.likeCount());
        String body = event.spotTitle() == null ? "" : event.spotTitle();

        return List.of(new NotificationCommand(
                event.diggingAuthorId(),
                NotificationType.DIGGING_POST_LIKE,
                title,
                body,
                Map.of("diggingId", event.diggingId(), "likerUserId", event.likerUserId()),
                null,
                null
        ));
    }
}
