package com.sodosiro.domain.course.repository;

import com.sodosiro.domain.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {

    Optional<Course> findByUserIdAndIsConfirmedFalse(Long userId);

    Optional<Course> findByIdAndUserId(Long id, Long userId);
}
