package com.sodosiro.domain.notification.repository;

import com.sodosiro.domain.notification.entity.Notification;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long>, NotificationQueryRepository {

    List<Notification> findByUserIdAndIdLessThanOrderByIdDesc(
            Long userId,
            Long cursor,
            org.springframework.data.domain.Pageable pageable
    );

    long countByUserIdAndIsReadFalse(Long userId);

    Optional<Notification> findByIdAndUserId(Long id, Long userId);

    void deleteAllByUserId(Long userId);
}
