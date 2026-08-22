package com.sodosiro.domain.notification.fcm;

import com.google.api.client.http.javanet.GzipDisablingHttpTransport;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import java.io.FileInputStream;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FirebaseConfig {

    public FirebaseConfig(@Value("${firebase.service-account-path:}") String serviceAccountPath) {
        if (serviceAccountPath == null || serviceAccountPath.isBlank()) {
            log.warn("FIREBASE_SERVICE_ACCOUNT_PATH가 없어 FCM 발송을 비활성화합니다.");
            return;
        }

        try (FileInputStream input = new FileInputStream(serviceAccountPath)) {
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(GoogleCredentials.fromStream(input))
                        .setHttpTransport(new GzipDisablingHttpTransport())
                        .build();
                FirebaseApp.initializeApp(options);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Firebase 서비스 계정 파일을 읽을 수 없습니다.", exception);
        }
    }
}
