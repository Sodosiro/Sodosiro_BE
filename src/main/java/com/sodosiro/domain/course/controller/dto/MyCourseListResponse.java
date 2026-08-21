package com.sodosiro.domain.course.controller.dto;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.entity.Course;
import java.time.LocalDate;
import java.util.List;

public record MyCourseListResponse(List<MyCourse> courses) {

    public record MyCourse(
            Long courseId,
            String displayName,
            LocalDate startDate,
            LocalDate endDate,
            CourseStatus status,
            String thumbnail
    ) {
        public static MyCourse from(Course course) {
            return new MyCourse(
                    course.getId(),
                    course.displayName(),
                    course.getStartDate(),
                    course.getEndDate(),
                    course.getStatus(),
                    thumbnail(course)
            );
        }

        private static String thumbnail(Course course) {
            return course.allSpots().stream()
                    .map(Course.SpotSnapshot::firstImage)
                    .filter(image -> image != null && !image.isBlank())
                    .findFirst()
                    .orElse(null);
        }
    }
}
