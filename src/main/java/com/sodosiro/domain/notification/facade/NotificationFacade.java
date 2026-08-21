package com.sodosiro.domain.notification.facade;

import com.sodosiro.domain.notification.command.NotificationCommand;

public interface NotificationFacade {

    boolean create(NotificationCommand command);
}
