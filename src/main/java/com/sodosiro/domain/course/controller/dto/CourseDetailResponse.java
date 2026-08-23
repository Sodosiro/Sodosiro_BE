package com.sodosiro.domain.course.controller.dto;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.entity.Course;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record CourseDetailResponse(
        Long courseId,
        LocalDate startDate,
        LocalDate endDate,
        CourseStatus status,
        List<DayDetail> days
) {
    public record DayDetail(int day, LocalDate date, List<SpotDetail> spots) {
    }

    public record SpotDetail(
            Long contentId,
            String title,
            String firstImage,
            BigDecimal mapX,
            BigDecimal mapY,
            Integer category,
            boolean mustVisit,
            boolean gpsVerified,
            boolean reviewWritten,
            Long reviewId
    ) {
    }

    /**
     * verifiedKeys는 "day:contentId" 형태로 이미 GPS 인증된 스팟을 표시하기 위한 키 집합이다.
     * reviewIdByContentId는 로그인 사용자가 해당 관광지에 이미 작성한 리뷰의 id(contentId 기준)다.
     */
    public static CourseDetailResponse from(
            Course course, Set<String> verifiedKeys, Map<Long, Long> reviewIdByContentId) {
        List<DayDetail> days = course.getDays().stream()
                .map(day -> new DayDetail(
                        day.day(),
                        day.date(),
                        day.spots().stream()
                                .map(spot -> {
                                    Long reviewId = reviewIdByContentId.get(spot.contentId());
                                    return new SpotDetail(
                                            spot.contentId(),
                                            spot.title(),
                                            spot.firstImage(),
                                            spot.mapX(),
                                            spot.mapY(),
                                            spot.category(),
                                            spot.mustVisit(),
                                            verifiedKeys.contains(day.day() + ":" + spot.contentId()),
                                            reviewId != null,
                                            reviewId);
                                })
                                .toList()))
                .toList();
        return new CourseDetailResponse(
                course.getId(), course.getStartDate(), course.getEndDate(), course.getStatus(), days);
    }
}
