package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.controller.dto.CourseDetailResponse;
import com.sodosiro.domain.course.controller.dto.MyCourseListResponse;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.repository.CourseRepository;
import com.sodosiro.domain.gps.entity.Gps;
import com.sodosiro.domain.gps.repository.GpsRepository;
import com.sodosiro.domain.review.entity.Review;
import com.sodosiro.domain.review.repository.ReviewRepository;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.payload.code.error.CourseErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 확정된 내 코스 목록/상세 조회 및 코스 삭제. 디깅 작성, GPS 인증 화면 등에서 사용한다. */
@Service
@RequiredArgsConstructor
public class CourseQueryService {

    private final CourseRepository courseRepository;
    private final GpsRepository gpsRepository;
    private final ReviewRepository reviewRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final ActiveCourseCacheWriter activeCourseCacheWriter;

    /** 미종료(UPCOMING/IN_PROGRESS)는 여행일이 임박한 순, 종료(FINISHED)는 최근에 끝난 순으로 정렬한다. */
    private static final Comparator<Course> MY_COURSE_ORDER = (a, b) -> {
        boolean aFinished = a.getStatus() == CourseStatus.FINISHED;
        boolean bFinished = b.getStatus() == CourseStatus.FINISHED;
        if (aFinished != bFinished) {
            return aFinished ? 1 : -1;
        }
        return aFinished
                ? b.getStartDate().compareTo(a.getStartDate())
                : a.getStartDate().compareTo(b.getStartDate());
    };

    /** draft(미확정)도 status가 항상 UPCOMING이라 자연스럽게 UPCOMING 필터/전체 조회에 포함된다. */
    @Transactional(readOnly = true)
    public MyCourseListResponse getMyCourses(Long userId, CourseStatus status) {
        List<Course> courses = (status == null
                        ? courseRepository.findByUserId(userId)
                        : courseRepository.findByUserIdAndStatus(userId, status))
                .stream()
                .sorted(MY_COURSE_ORDER)
                .toList();

        Map<Long, String> sigunguCodeByContentId = resolveSigunguCodes(courses);

        return new MyCourseListResponse(
                courses.stream()
                        .map(course -> MyCourseListResponse.MyCourse.from(course, sigunguCodeByContentId))
                        .toList());
    }

    /** 코스는 시군구코드를 직접 저장하지 않으므로, 각 코스 첫 스팟의 TouristSpot.ldongSignguCode로 지역을 알아낸다. */
    private Map<Long, String> resolveSigunguCodes(List<Course> courses) {
        List<Long> firstContentIds = courses.stream()
                .map(course -> course.allSpots().isEmpty() ? null : course.allSpots().getFirst().contentId())
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return touristSpotRepository.findAllById(firstContentIds).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, TouristSpot::getLdongSignguCode));
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long userId, Long courseId) {
        Course course = courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new GeneralException(CourseErrorCode._COURSE_NOT_FOUND));

        Set<String> verifiedKeys = gpsRepository.findByCourseId(courseId).stream()
                .map(gps -> gps.getDay() + ":" + gps.getContentId())
                .collect(Collectors.toSet());

        List<Long> contentIds = course.allSpots().stream()
                .map(Course.SpotSnapshot::contentId)
                .distinct()
                .toList();
        Map<Long, Long> reviewIdByContentId = reviewRepository
                .findByUserIdAndContentIdInAndIsDeletedFalse(userId, contentIds).stream()
                .collect(Collectors.toMap(Review::getContentId, Review::getId));

        Map<Long, TouristSpot> touristSpotByContentId = touristSpotRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, spot -> spot));

        return CourseDetailResponse.from(course, verifiedKeys, reviewIdByContentId, touristSpotByContentId);
    }

    /** 상태(draft/확정/진행중/완료) 상관없이 삭제 가능하다. GPS 인증·디깅 기록은 건드리지 않는다. */
    @Transactional
    public void deleteCourse(Long userId, Long courseId) {
        Course course = courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new GeneralException(CourseErrorCode._COURSE_NOT_FOUND));
        courseRepository.delete(course);
        activeCourseCacheWriter.evict(userId);
    }
}
