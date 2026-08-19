package com.sodosiro.domain.course.constants;

public enum CourseStatus {
    UPCOMING,     // 예정 (확정되었으나 시작일이 미래인 상태)
    IN_PROGRESS,  // 진행 중 (오늘 날짜가 시작일과 종료일 사이에 포함된 상태)
    FINISHED // 종료 (사용자가 종료 버튼을 눌렀거나 기간이 지난 상태)
}
