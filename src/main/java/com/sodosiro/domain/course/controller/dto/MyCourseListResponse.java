package com.sodosiro.domain.course.controller.dto;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.entity.Course;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record MyCourseListResponse(List<MyCourse> courses) {

    public record MyCourse(
            Long courseId,
            String title,
            String displayName,
            LocalDate startDate,
            LocalDate endDate,
            CourseStatus status,
            boolean isConfirmed,
            String sigunguCode,
            String thumbnail,
            LocalDateTime createdAt
    ) {
        /** sigunguCodeByContentId는 코스 첫 스팟의 contentId로 조회한 TouristSpot.ldongSignguCode 맵이다. */
        public static MyCourse from(Course course, Map<Long, String> sigunguCodeByContentId) {
            return new MyCourse(
                    course.getId(),
                    course.getTitle(),
                    course.displayName(),
                    course.getStartDate(),
                    course.getEndDate(),
                    course.getStatus(),
                    course.getIsConfirmed(),
                    sigunguCodeByContentId.get(firstContentId(course)),
                    thumbnail(course),
                    course.getCreatedAt()
            );
        }

        private static Long firstContentId(Course course) {
            List<Course.SpotSnapshot> spots = course.allSpots();
            return spots.isEmpty() ? null : spots.getFirst().contentId();
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
