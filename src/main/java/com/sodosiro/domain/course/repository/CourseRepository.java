package com.sodosiro.domain.course.repository;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByUserIdAndIsConfirmedFalse(Long userId);

    Optional<Course> findByIdAndUserId(Long id, Long userId);

    List<Course> findByUserIdOrderByIdDesc(Long userId);

    List<Course> findByUserIdAndStatusOrderByIdDesc(Long userId, CourseStatus status);

    List<Course> findByIsConfirmedTrueAndStatusAndStartDateLessThanEqual(CourseStatus status, LocalDate date);

    List<Course> findByIsConfirmedTrueAndStatusAndEndDateLessThan(CourseStatus status, LocalDate date);

    /** FINISHED가 아닌(예정/진행 중) 확정 코스 중, [startDate, endDate]와 기간이 겹치는 게 있는지 확인한다. */
    boolean existsByUserIdAndIsConfirmedTrueAndStatusNotAndStartDateLessThanEqualAndEndDateGreaterThanEqual(Long userId, CourseStatus status, LocalDate startDate, LocalDate endDate);

    long deleteAllByUserId(Long userId);
}
