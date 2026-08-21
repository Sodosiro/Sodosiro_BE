package com.sodosiro.domain.notification.service;

import com.sodosiro.domain.notification.entity.NotificationPreference;
import com.sodosiro.domain.notification.repository.NotificationPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저별 푸시 수신 on/off. 앱 내 알림(Notification) 저장 여부와는 무관하다 — 푸시 전송만 제어한다. */
@Service
@RequiredArgsConstructor
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    /** 토글한 적 없는 유저는 기본값(수신 on)으로 취급한다. */
    @Transactional(readOnly = true)
    public boolean isPushEnabled(Long userId) {
        return preferenceRepository.findById(userId)
                .map(NotificationPreference::isPushEnabled)
                .orElse(true);
    }

    @Transactional
    public boolean updatePushEnabled(Long userId, boolean pushEnabled) {
        NotificationPreference preference = preferenceRepository.findById(userId)
                .orElseGet(() -> NotificationPreference.create(userId, pushEnabled));
        preference.updatePushEnabled(pushEnabled);
        preferenceRepository.save(preference);
        return preference.isPushEnabled();
    }
}
