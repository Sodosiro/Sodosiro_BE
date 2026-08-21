package com.sodosiro.domain.notification.trigger;

/**
 * 여행이 끝난 코스 1건에 대한 리뷰 작성 유도 트리거.
 * 장소 1건이건 10건이건 그냥 여행이 끝나면 이벤트르 보냄
 */
public record ReviewRequestTargetEvent(
        Long userId,
        Long courseId,
        String courseName,       // 대표 스팟으로 합성 (예: "영진횟집 외 9곳") 따로 코스의 대한 별칭이 없음
        int pendingSpotCount     // 아직 리뷰를 안 쓴 대상 장소 수
) implements NotificationTrigger {
}
