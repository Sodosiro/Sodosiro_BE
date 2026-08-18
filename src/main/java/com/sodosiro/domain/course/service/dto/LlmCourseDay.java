package com.sodosiro.domain.course.service.dto;

import java.util.List;

public record LlmCourseDay(int day, List<Long> contentIds) {
}
