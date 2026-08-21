package com.sodosiro.domain.notification.trigger;

import com.sodosiro.domain.notification.facade.NotificationFacade;
import com.sodosiro.domain.notification.factory.NotificationFactoryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationTriggerListener {

    private final NotificationFactoryRegistry registry;
    private final NotificationFacade facade;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void on(NotificationTrigger trigger) {
        registry.create(trigger, facade);
    }
}
