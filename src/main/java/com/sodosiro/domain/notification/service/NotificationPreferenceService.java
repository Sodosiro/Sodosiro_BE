package com.sodosiro.domain.notification.service;

import com.sodosiro.domain.notification.entity.NotificationPreference;
import com.sodosiro.domain.notification.entity.NotificationPreferenceType;
import com.sodosiro.domain.notification.entity.NotificationType;
import com.sodosiro.domain.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저별 알림 수신 설정(전체/타입별) 조회·토글. 앱 내 알림(Notification) 저장 여부와는 무관하다 — FCM 전송만 제어한다. */
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    /** 토글한 적 없는 유저는 기본값(전체 수신 on)으로 취급한다. */
    @Transactional(readOnly = true)
    public NotificationPreference getPreference(Long userId) {
        return preferenceRepository.findById(userId)
                .orElseGet(() -> NotificationPreference.createDefault(userId));
    }

    @Transactional(readOnly = true)
    public boolean isPushEnabled(Long userId, NotificationType type) {
        return getPreference(userId).isPushEnabled(type);
    }

    @Transactional
    public NotificationPreference toggle(Long userId, NotificationPreferenceType type, boolean enabled) {
        NotificationPreference preference = preferenceRepository.findById(userId)
                .orElseGet(() -> NotificationPreference.createDefault(userId));
        preference.updateEnabled(type, enabled);
        return preferenceRepository.save(preference);
    }
}
