package com.sodosiro.domain.notification.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.entity.QCourse;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CourseConfirmReminderQueryRepositoryImpl implements CourseConfirmReminderQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QCourse c = QCourse.course;

    @Override
    public List<Course> findUnconfirmedCoursesStartingOn(LocalDate startDate) {
        return queryFactory
                .selectFrom(c)
                .where(
                        c.isConfirmed.isFalse(),
                        c.startDate.eq(startDate)
                )
                .orderBy(c.id.asc())
                .fetch();
    }
}
