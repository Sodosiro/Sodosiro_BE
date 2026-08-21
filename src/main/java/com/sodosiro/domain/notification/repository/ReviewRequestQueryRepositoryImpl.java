package com.sodosiro.domain.notification.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.entity.QCourse;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ReviewRequestQueryRepositoryImpl implements ReviewRequestQueryRepository {

    private final JPAQueryFactory queryFactory;
    private static final QCourse c = QCourse.course;

    /**
     * {@code finished_at} 이 NULL 인 코스(전환 로직 배포 이전에 이미 종료된 과거분)는 두 비교 조건에
     * 모두 걸리지 않아 자동으로 제외된다 — 과거분 소급 발송 방지.
     */
    @Override
    public List<Course> findReviewRequestTargets(LocalDateTime finishedBefore, LocalDateTime finishedAfter) {
        return queryFactory
                .selectFrom(c)
                .where(
                        c.status.eq(CourseStatus.FINISHED),
                        c.finishedAt.before(finishedBefore),
                        c.finishedAt.goe(finishedAfter)
                )
                .orderBy(c.id.asc())
                .fetch();
    }
}
