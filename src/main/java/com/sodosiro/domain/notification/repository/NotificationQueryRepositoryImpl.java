package com.sodosiro.domain.notification.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.notification.entity.QNotification;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationQueryRepositoryImpl implements NotificationQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QNotification n = QNotification.notification;

    @Override
    public long markAllRead(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        return queryFactory
                .update(n)
                .set(n.isRead, true)
                .set(n.readAt, now)
                .set(n.updatedAt, now)
                .where(
                        n.userId.eq(userId),
                        n.isRead.isFalse()
                )
                .execute();
    }
}
