package com.sodosiro.domain.notification.service.dto;

/**
 * 배치 1회 실행 결과. DAG 로그에서 실행 결과를 확인하는 용도다.
 *
 * @param targetedCourses      조건 1~3 을 통과해 알림을 시도한 코스 수
 * @param createdNotifications 실제로 만들어져 발송된 알림 수
 * @param skippedByCooldown    쿨다운 가드에 막혀 건너뛴 수 (= 이미 보낸 코스)
 */
public record ReviewRequestBatchResult(
        int targetedCourses,
        int createdNotifications,
        int skippedByCooldown
) {
}
