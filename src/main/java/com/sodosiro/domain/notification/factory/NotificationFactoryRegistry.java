package com.sodosiro.domain.notification.factory;

import com.sodosiro.domain.notification.command.NotificationCommand;
import com.sodosiro.domain.notification.facade.NotificationFacade;
import com.sodosiro.domain.notification.trigger.NotificationTrigger;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationFactoryRegistry {

    private final List<NotificationFactory<?>> factories;

    @SuppressWarnings("unchecked")
    public int create(NotificationTrigger trigger, NotificationFacade facade) {
        Optional<NotificationFactory<NotificationTrigger>> factory = factories.stream()
                .filter(candidate -> candidate.triggerType().isInstance(trigger))
                .findFirst()
                .map(candidate -> (NotificationFactory<NotificationTrigger>) candidate);
        if (factory.isEmpty()) {
            return 0;
        }

        int created = 0;
        for (NotificationCommand command : factory.get().create(trigger)) {
            if (facade.create(command)) {
                created++;
            }
        }
        return created;
    }
}
