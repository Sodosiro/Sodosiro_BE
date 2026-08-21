package com.sodosiro.domain.course.controller.dto;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.entity.Course;

public record CourseStartResponse(Long courseId, CourseStatus status) {
    public static CourseStartResponse from(Course course) {
        return new CourseStartResponse(course.getId(), course.getStatus());
    }
}
