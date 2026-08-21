package com.sodosiro.domain.notification.service;

import com.sodosiro.domain.notification.controller.dto.NotificationListResponse;
import com.sodosiro.domain.notification.controller.dto.NotificationResponse;
import com.sodosiro.domain.notification.entity.Notification;
import com.sodosiro.domain.notification.repository.NotificationRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class NotificationQueryService {

    private static final long CURSOR_START = Long.MAX_VALUE;
    private final NotificationRepository notificationRepository;

    @Transactional(readOnly = true)
    public NotificationListResponse list(Long userId, String cursor, int size) {
        if (size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size는 1에서 100 사이여야 합니다.");
        List<Notification> fetched = notificationRepository.findByUserIdAndIdLessThanOrderByIdDesc(userId, parseCursor(cursor), PageRequest.of(0, size + 1));
        boolean hasNext = fetched.size() > size;
        List<Notification> items = hasNext ? fetched.subList(0, size) : fetched;
        String nextCursor = hasNext ? String.valueOf(items.getLast().getId()) : null;

        return new NotificationListResponse(
                items.stream().map(NotificationResponse::from).toList(),
                nextCursor,
                hasNext,
                notificationRepository.countByUserIdAndIsReadFalse(userId)
        );
    }

    @Transactional
    public void markRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."));
        notification.markRead();
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
    }

    private long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return CURSOR_START;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 cursor입니다.");
        }
    }
}
