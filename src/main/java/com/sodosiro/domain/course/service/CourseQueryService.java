package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.controller.dto.MyCourseListResponse;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.repository.CourseRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 확정된 내 코스 목록 조회. 디깅 작성 시 코스를 먼저 선택하는 depth 에서 사용한다. */
@Service
@RequiredArgsConstructor
public class CourseQueryService {

    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public MyCourseListResponse getMyCourses(Long userId, CourseStatus status) {
        List<Course> courses = status == null
                ? courseRepository.findByUserIdAndIsConfirmedTrueOrderByIdDesc(userId)
                : courseRepository.findByUserIdAndIsConfirmedTrueAndStatusOrderByIdDesc(userId, status);

        return new MyCourseListResponse(
                courses.stream().map(MyCourseListResponse.MyCourse::from).toList());
    }
}
