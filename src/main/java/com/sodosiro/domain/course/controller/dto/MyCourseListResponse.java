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
                    displayName(course),
                    course.getStartDate(),
                    course.getEndDate(),
                    course.getStatus(),
                    thumbnail(course)
            );
        }

        /** 코스에 이름 필드가 없어 대표 스팟명 + 스팟 수로 합성한다 (예: "설악산 국립공원 외 3곳"). */
        private static String displayName(Course course) {
            List<Course.SpotSnapshot> spots = allSpots(course);
            if (spots.isEmpty()) {
                return "여행 코스";
            }
            String first = spots.getFirst().title();
            return spots.size() == 1 ? first : "%s 외 %d곳".formatted(first, spots.size() - 1);
        }

        private static String thumbnail(Course course) {
            return allSpots(course).stream()
                    .map(Course.SpotSnapshot::firstImage)
                    .filter(image -> image != null && !image.isBlank())
                    .findFirst()
                    .orElse(null);
        }

        private static List<Course.SpotSnapshot> allSpots(Course course) {
            if (course.getDays() == null) {
                return List.of();
            }
            return course.getDays().stream()
                    .flatMap(day -> day.spots().stream())
                    .toList();
        }
    }
}
