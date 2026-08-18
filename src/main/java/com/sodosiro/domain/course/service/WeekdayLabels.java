package com.sodosiro.domain.course.service;

import com.sodosiro.domain.travel.entity.TouristSpot;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Map;

/** 날짜 -&gt; 요일 라벨 변환과 휴무일 판정을 규칙기반 경로/AI 경로가 공통으로 쓰기 위한 유틸리티. */
final class WeekdayLabels {

    private static final Map<DayOfWeek, String> LABELS = Map.of(
            DayOfWeek.MONDAY, "월요일",
            DayOfWeek.TUESDAY, "화요일",
            DayOfWeek.WEDNESDAY, "수요일",
            DayOfWeek.THURSDAY, "목요일",
            DayOfWeek.FRIDAY, "금요일",
            DayOfWeek.SATURDAY, "토요일",
            DayOfWeek.SUNDAY, "일요일"
    );

    private WeekdayLabels() {
    }

    static String labelOf(LocalDate date) {
        return LABELS.get(date.getDayOfWeek());
    }

    /** 휴무일이 아니면 true */
    static boolean isOpenOn(TouristSpot spot, String weekdayLabel) {
        String restdate = spot.getRestdate();
        return restdate == null || !restdate.contains(weekdayLabel);
    }
}
