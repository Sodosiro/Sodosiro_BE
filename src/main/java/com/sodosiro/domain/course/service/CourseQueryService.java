package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.controller.dto.CourseDetailResponse;
import com.sodosiro.domain.course.controller.dto.MyCourseListResponse;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.repository.CourseRepository;
import com.sodosiro.domain.gps.entity.Gps;
import com.sodosiro.domain.gps.repository.GpsRepository;
import com.sodosiro.global.payload.code.error.CourseErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import java.util.List;
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

    @Transactional(readOnly = true)
    public MyCourseListResponse getMyCourses(Long userId, CourseStatus status) {
        List<Course> courses = status == null
                ? courseRepository.findByUserIdAndIsConfirmedTrueOrderByIdDesc(userId)
                : courseRepository.findByUserIdAndIsConfirmedTrueAndStatusOrderByIdDesc(userId, status);

        return new MyCourseListResponse(
                courses.stream().map(MyCourseListResponse.MyCourse::from).toList());
    }

    @Transactional(readOnly = true)
    public CourseDetailResponse getCourseDetail(Long userId, Long courseId) {
        Course course = courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new GeneralException(CourseErrorCode._COURSE_NOT_FOUND));

        Set<String> verifiedKeys = gpsRepository.findByCourseId(courseId).stream()
                .map(gps -> gps.getDay() + ":" + gps.getContentId())
                .collect(Collectors.toSet());

        return CourseDetailResponse.from(course, verifiedKeys);
    }

    /** 상태(draft/확정/진행중/완료) 상관없이 삭제 가능하다. GPS 인증·디깅 기록은 건드리지 않는다. */
    @Transactional
    public void deleteCourse(Long userId, Long courseId) {
        Course course = courseRepository.findByIdAndUserId(courseId, userId)
                .orElseThrow(() -> new GeneralException(CourseErrorCode._COURSE_NOT_FOUND));
        courseRepository.delete(course);
    }
}
