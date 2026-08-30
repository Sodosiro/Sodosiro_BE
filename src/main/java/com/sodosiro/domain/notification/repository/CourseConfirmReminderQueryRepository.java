package com.sodosiro.domain.notification.repository;

import com.sodosiro.domain.course.entity.Course;
import java.time.LocalDate;
import java.util.List;

public interface CourseConfirmReminderQueryRepository {

    List<Course> findUnconfirmedCoursesStartingOn(LocalDate startDate);
}
