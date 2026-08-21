package com.sodosiro.domain.notification.repository;

import com.sodosiro.domain.notification.entity.UserDevice;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserDeviceRepository extends JpaRepository<UserDevice, Long> {

    Optional<UserDevice> findByUserIdAndDeviceId(Long userId, String deviceId);

    Optional<UserDevice> findByFcmToken(String fcmToken);

    Optional<UserDevice> findByUserIdAndDeviceIdAndInvalidatedAtIsNull(Long userId, String deviceId);

    List<UserDevice> findByUserIdAndPushEnabledTrueAndInvalidatedAtIsNullAndFcmTokenIsNotNull(Long userId);
}
