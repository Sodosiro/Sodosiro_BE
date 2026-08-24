package com.sodosiro.domain.notification.service;

import com.sodosiro.domain.notification.entity.Notification;
import com.sodosiro.domain.notification.entity.UserDevice;
import com.sodosiro.domain.notification.fcm.FcmSendResult;
import com.sodosiro.domain.notification.fcm.FcmSender;
import com.sodosiro.domain.notification.repository.NotificationRepository;
import com.sodosiro.domain.notification.trigger.NotificationCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;


@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationPushListener {

    private final NotificationRepository notificationRepository;
    private final UserDeviceService userDeviceService;
    private final NotificationPreferenceService notificationPreferenceService;
    private final FcmSender fcmSender;

    @Async("fcmPushExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(NotificationCreatedEvent event) {
        Notification notification = notificationRepository.findById(event.notificationId()).orElse(null);
        if (notification == null) {
            return;
        }
        if (!notificationPreferenceService.isPushEnabled(event.userId(), notification.getType())) {
            log.debug("푸시 수신 꺼짐, FCM 전송 생략: userId={}, notificationId={}, type={}",
                    event.userId(), event.notificationId(), notification.getType());
            return;
        }
        for (UserDevice device : userDeviceService.activeDevices(event.userId())) {
            FcmSendResult result = fcmSender.send(device, notification);
            if (result == FcmSendResult.INVALID_TOKEN) {
                device.invalidate();
            }
        }
    }
}
