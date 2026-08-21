package com.sodosiro.domain.notification.facade;

import com.sodosiro.domain.notification.command.NotificationCommand;

public interface NotificationFacade {
    void create(NotificationCommand command);
}
