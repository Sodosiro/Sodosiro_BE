package com.sodosiro.domain.course.controller.dto;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.route.constants.TransportMode;
import com.sodosiro.domain.travel.entity.TouristSpot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
            boolean gpsVerified,
            boolean reviewWritten,
            Long reviewId,
            String overview,
            BigDecimal avgRating,
            Integer reviewCount
    ) {
    }

    /**
     * verifiedKeys는 "day:contentId" 형태로 이미 GPS 인증된 스팟을 표시하기 위한 키 집합이다.
     * reviewIdByContentId는 로그인 사용자가 해당 관광지에 이미 작성한 리뷰의 id(contentId 기준)다.
     * touristSpotByContentId는 overview/avgRating/reviewCount 등 최신 스팟 정보를 채우기 위한 조회 결과다.
     */
    public static CourseDetailResponse from(
            Course course, Set<String> verifiedKeys, Map<Long, Long> reviewIdByContentId,
            Map<Long, TouristSpot> touristSpotByContentId) {
        List<DayDetail> days = course.getDays().stream()
                .map(day -> new DayDetail(
                        day.day(),
                        day.date(),
                        day.spots().stream()
                                .map(spot -> {
                                    Long reviewId = reviewIdByContentId.get(spot.contentId());
                                    TouristSpot touristSpot = touristSpotByContentId.get(spot.contentId());
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
                                            reviewId,
                                            touristSpot == null ? null : touristSpot.getOverview(),
                                            touristSpot == null ? null : touristSpot.getAvgRating(),
                                            touristSpot == null ? null : touristSpot.getReviewCount());
                                })
                                .toList()))
                .toList();
        return new CourseDetailResponse(
                course.getId(), course.getTitle(), course.getStartDate(), course.getEndDate(),
                course.getTransportMode(), course.getStatus(), days,
                course.getCarRoutes(), course.getTransitRoutes());
    }
}
