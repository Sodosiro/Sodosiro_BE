package com.sodosiro.domain.notification.service.dto;

import com.sodosiro.domain.notification.trigger.ReviewRequestTargetEvent;

/**
 * 조건 1~3(설계 문서 3-1)을 모두 통과한 코스 1건. 여기까지 오면 알림 1건이 나간다.
 * */
public record ReviewRequestTarget(
        Long userId,
        Long courseId,
        String courseName,
        int pendingSpotCount
) {
    public ReviewRequestTargetEvent toEvent() {
        return new ReviewRequestTargetEvent(userId, courseId, courseName, pendingSpotCount);
    }
}
