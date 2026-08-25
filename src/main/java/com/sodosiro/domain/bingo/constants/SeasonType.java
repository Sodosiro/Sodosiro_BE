package com.sodosiro.domain.bingo.constants;

import java.time.LocalDate;
import java.time.YearMonth;

/** 3개월 주기 계절 구분. WINTER는 12월~다음해 2월로 연도 경계를 넘는다. */
public enum SeasonType {
    SPRING(3, 5, "봄"),
    SUMMER(6, 8, "여름"),
    FALL(9, 11, "가을"),
    WINTER(12, 2, "겨울");

    private final int startMonth;
    private final int endMonth;
    private final String koreanLabel;

    SeasonType(int startMonth, int endMonth, String koreanLabel) {
        this.startMonth = startMonth;
        this.endMonth = endMonth;
        this.koreanLabel = koreanLabel;
    }

    public String koreanLabel() {
        return koreanLabel;
    }

    public static SeasonType of(LocalDate date) {
        return switch (date.getMonthValue()) {
            case 3, 4, 5 -> SPRING;
            case 6, 7, 8 -> SUMMER;
            case 9, 10, 11 -> FALL;
            default -> WINTER;
        };
    }

    /** WINTER는 12월 기준 연도를 시즌 연도로 취급한다 (예: 2026-01은 WINTER 2025). */
    public static int seasonYearOf(LocalDate date) {
        int month = date.getMonthValue();
        return (month == 1 || month == 2) ? date.getYear() - 1 : date.getYear();
    }

    public LocalDate startDateOf(int seasonYear) {
        return LocalDate.of(seasonYear, startMonth, 1);
    }

    public LocalDate endDateOf(int seasonYear) {
        int endYear = this == WINTER ? seasonYear + 1 : seasonYear;
        int lastDay = YearMonth.of(endYear, endMonth).lengthOfMonth();
        return LocalDate.of(endYear, endMonth, lastDay);
    }
}
