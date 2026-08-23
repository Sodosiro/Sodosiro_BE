package com.sodosiro.domain.course.controller.dto;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.route.dto.TransportMode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record CourseDetailResponse(
        Long courseId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        TransportMode transportMode,
        CourseStatus status,
        List<DayDetail> days,
        List<Course.DayCarRoute> carRoutes,
        List<Course.DayPublicTransportRoute> transitRoutes
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
            boolean gpsVerified
    ) {
    }

    /** verifiedKeys는 "day:contentId" 형태로 이미 GPS 인증된 스팟을 표시하기 위한 키 집합이다. */
    public static CourseDetailResponse from(Course course, Set<String> verifiedKeys) {
        List<DayDetail> days = course.getDays().stream()
                .map(day -> new DayDetail(
                        day.day(),
                        day.date(),
                        day.spots().stream()
                                .map(spot -> new SpotDetail(
                                        spot.contentId(),
                                        spot.title(),
                                        spot.firstImage(),
                                        spot.mapX(),
                                        spot.mapY(),
                                        spot.category(),
                                        spot.mustVisit(),
                                        verifiedKeys.contains(day.day() + ":" + spot.contentId())))
                                .toList()))
                .toList();
        return new CourseDetailResponse(
                course.getId(), course.getTitle(), course.getStartDate(), course.getEndDate(),
                course.getTransportMode(), course.getStatus(), days,
                course.getCarRoutes(), course.getTransitRoutes());
    }
}
