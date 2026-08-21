package com.sodosiro.domain.notification.facade;

import com.sodosiro.domain.notification.command.NotificationCommand;
import com.sodosiro.domain.notification.entity.Notification;
import com.sodosiro.domain.notification.repository.NotificationDeliveryGuardRepository;
import com.sodosiro.domain.notification.repository.NotificationRepository;
import com.sodosiro.domain.notification.trigger.NotificationCreatedEvent;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationFacadeImpl implements NotificationFacade {
    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryGuardRepository guardRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public boolean create(NotificationCommand command) {
        LocalDateTime now = LocalDateTime.now();
        if (command.dedupeKey() != null
                && command.cooldownUntil() != null
                && guardRepository.acquire(
                        command.recipientUserId(),
                        command.type().name(),
                        command.dedupeKey(),
                        now,
                        command.cooldownUntil()
                ) == 0) {
            return false;
        }

        Notification notification = notificationRepository.save(Notification.create(
                command.recipientUserId(),
                command.type(),
                command.title(),
                command.body(),
                command.payload()
        ));
        eventPublisher.publishEvent(new NotificationCreatedEvent(notification.getId(), notification.getUserId()));
        return true;
    }
}
