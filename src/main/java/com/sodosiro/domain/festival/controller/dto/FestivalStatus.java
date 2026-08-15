package com.sodosiro.domain.festival.controller.dto;

import java.time.LocalDate;

/**
 * 축제 진행 상태. 조회 필터로 쓰이며, 개별 축제의 상태는 KST 기준 오늘 날짜로 파생한다.
 * {@link #ALL}은 필터 입력 전용이며 개별 축제의 상태로는 반환되지 않는다.
 */
public enum FestivalStatus {
    /** 전체 (필터 입력 전용) */
    ALL,
    /** 진행중: 오늘이 시작일~끝일 사이 */
    ONGOING,
    /** 예정: 시작일이 오늘 이후 */
    UPCOMING,
    /** 종료: 끝일이 오늘 이전 */
    ENDED;

    /** 개별 축제의 상태를 KST 기준 오늘 날짜로 판정한다. ALL은 반환하지 않는다. */
    public static FestivalStatus resolve(LocalDate startDate, LocalDate endDate, LocalDate today) {
        if (startDate.isAfter(today)) {
            return UPCOMING;
        }
        if (endDate.isBefore(today)) {
            return ENDED;
        }
        return ONGOING;
    }
}
