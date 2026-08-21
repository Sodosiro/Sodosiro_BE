package com.sodosiro.domain.notification.repository;

import com.sodosiro.domain.notification.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {
}
