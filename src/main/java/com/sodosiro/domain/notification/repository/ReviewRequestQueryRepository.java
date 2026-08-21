package com.sodosiro.domain.notification.repository;

import com.sodosiro.domain.course.entity.Course;
import java.time.LocalDateTime;
import java.util.List;

public interface ReviewRequestQueryRepository {

    /**
     * 종료된 지 충분히 지났고 아직 너무 오래되지는 않은 코스를 모두 가져온다.
     *
     * @param finishedBefore 이 시각보다 먼저 종료된 코스만
     * @param finishedAfter  이 시각 이후에 종료된 코스만 수개월 전 코스까지 소급 발송되는 것을 막는 상한선이다.
     */
    List<Course> findReviewRequestTargets(LocalDateTime finishedBefore, LocalDateTime finishedAfter);
}
