package com.sodosiro.domain.notification.factory;

import com.sodosiro.domain.notification.command.NotificationCommand;
import com.sodosiro.domain.notification.trigger.NotificationTrigger;
import java.util.List;

public interface NotificationFactory<T extends NotificationTrigger> {
    Class<T> triggerType();

    List<NotificationCommand> create(T trigger);
}
