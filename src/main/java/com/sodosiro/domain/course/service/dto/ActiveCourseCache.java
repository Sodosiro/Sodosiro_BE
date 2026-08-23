package com.sodosiro.domain.course.service.dto;

import java.time.LocalDate;
import java.util.List;


public record ActiveCourseCache(
        Long courseId,
        LocalDate startDate,
        LocalDate endDate,
        List<Long> scheduledContentIds
) {

    public static String redisKey(Long userId) {
        return "user:%d:active-course".formatted(userId);
    }
}
