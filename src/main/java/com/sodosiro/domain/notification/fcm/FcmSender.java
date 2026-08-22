package com.sodosiro.domain.notification.fcm;

import com.sodosiro.domain.notification.entity.Notification;
import com.sodosiro.domain.notification.entity.UserDevice;

public interface FcmSender {

    FcmSendResult send(UserDevice device, Notification notification);
}
