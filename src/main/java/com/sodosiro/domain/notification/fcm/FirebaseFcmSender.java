package com.sodosiro.domain.notification.fcm;

import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.sodosiro.domain.notification.entity.Notification;
import com.sodosiro.domain.notification.entity.UserDevice;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FirebaseFcmSender implements FcmSender {

    @Override
    public FcmSendResult send(UserDevice device, Notification notification) {
        if (FirebaseApp.getApps().isEmpty()) {
            log.warn("FCM 미발송: FirebaseApp 이 초기화되지 않음(서비스 계정 경로/재시작 확인).");
            return FcmSendResult.RETRYABLE_FAILURE;
        }

        try {
            Message message = Message.builder()
                    .setToken(device.getFcmToken())
                    .setNotification(com.google.firebase.messaging.Notification.builder()
                            .setTitle(notification.getTitle())
                            .setBody(notification.getBody())
                            .build())
                    .putAllData(data(notification))
                    .build();
            FirebaseMessaging.getInstance().send(message);
            return FcmSendResult.ACCEPTED;
        } catch (FirebaseMessagingException exception) {
            log.warn("FCM 발송 실패(FirebaseMessagingException). errorCode={}, messagingErrorCode={}, message={}",
                    exception.getErrorCode(), exception.getMessagingErrorCode(), exception.getMessage());
            return getFcmSendResult(exception);
        } catch (RuntimeException exception) {
            log.warn("FCM 발송 실패(RuntimeException).", exception);
            return FcmSendResult.RETRYABLE_FAILURE;
        }
    }

    private static @NonNull FcmSendResult getFcmSendResult(FirebaseMessagingException exception) {
        return exception.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED
                || exception.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT
                ? FcmSendResult.INVALID_TOKEN
                : FcmSendResult.RETRYABLE_FAILURE;
    }


    private Map<String, String> data(Notification notification) {
        Map<String, String> data = new HashMap<>();
        data.put("notificationId", String.valueOf(notification.getId()));
        data.put("type", notification.getType().name());
        notification.getPayload().forEach((key, value) -> data.put(key, String.valueOf(value)));
        return data;
    }
}
