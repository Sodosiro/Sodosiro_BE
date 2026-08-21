package com.sodosiro.domain.notification.service;

import com.sodosiro.domain.notification.controller.dto.PushTokenUpsertRequest;
import com.sodosiro.domain.notification.entity.UserDevice;
import com.sodosiro.domain.notification.repository.UserDeviceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserDeviceService {
    private final UserDeviceRepository deviceRepository;

    @Transactional
    public void upsert(Long userId, String deviceId, PushTokenUpsertRequest request) {
        deviceRepository.findByFcmToken(request.fcmToken())
                .filter(holder -> holder.filterOutSameUserAndDevice(userId, deviceId))
                .ifPresent(UserDevice::detachToken);

        UserDevice device = deviceRepository.findByUserIdAndDeviceId(userId, deviceId)
                .orElseGet(() -> UserDevice.create(userId, deviceId, request.fcmToken(), request.platform()));
        device.refresh(request.fcmToken(), request.platform());
        deviceRepository.save(device);
    }

    @Transactional
    public void deactivate(Long userId, String deviceId) {
        deviceRepository.findByUserIdAndDeviceIdAndInvalidatedAtIsNull(userId, deviceId)
                .ifPresent(UserDevice::invalidate);
    }

    @Transactional(readOnly = true)
    public List<UserDevice> activeDevices(Long userId) {
        return deviceRepository.findByUserIdAndPushEnabledTrueAndInvalidatedAtIsNullAndFcmTokenIsNotNull(userId);
    }
}
