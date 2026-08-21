package com.sodosiro.domain.notification.factory;

import com.sodosiro.domain.notification.facade.NotificationFacade;
import com.sodosiro.domain.notification.trigger.NotificationTrigger;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationFactoryRegistry {

    private final List<NotificationFactory<?>> factories;

    @SuppressWarnings("unchecked")
    public void create(NotificationTrigger trigger, NotificationFacade facade) {
        factories.stream()
                .filter(factory -> factory.triggerType().isInstance(trigger))
                .findFirst()
                .map(factory -> (NotificationFactory<NotificationTrigger>) factory)
                .ifPresent(factory -> factory.create(trigger).forEach(facade::create));
    }
}
