package com.sodosiro.domain.notification.repository;

import com.sodosiro.domain.notification.entity.NotificationDeliveryGuard;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryGuardRepository extends JpaRepository<NotificationDeliveryGuard, NotificationDeliveryGuard.Id> {
    @Modifying
    @Query(value = "insert into notification_delivery_guard (user_id, type, dedupe_key, last_sent_at, cooldown_until) values (:userId, cast(:type as varchar), :dedupeKey, :now, :cooldownUntil) on conflict (user_id, type, dedupe_key) do update set last_sent_at = excluded.last_sent_at, cooldown_until = excluded.cooldown_until where notification_delivery_guard.cooldown_until <= :now", nativeQuery = true)
    int acquire(@Param("userId") Long userId, @Param("type") String type, @Param("dedupeKey") String dedupeKey, @Param("now") LocalDateTime now, @Param("cooldownUntil") LocalDateTime cooldownUntil);
}
