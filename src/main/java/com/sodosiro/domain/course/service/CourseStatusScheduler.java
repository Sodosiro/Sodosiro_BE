package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.repository.CourseRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 매일 00시, 확정된 코스의 여행 상태를 오늘 날짜 기준으로 자동 전환한다 (UPCOMING→IN_PROGRESS, IN_PROGRESS→FINISHED). */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseStatusScheduler {

    private final CourseRepository courseRepository;

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void updateStatuses() {
        LocalDate today = LocalDate.now();

        List<Course> toStart = courseRepository.findByIsConfirmedTrueAndStatusAndStartDateLessThanEqual(CourseStatus.UPCOMING, today);
        toStart.forEach(Course::start);

        List<Course> toFinish = courseRepository.findByIsConfirmedTrueAndStatusAndEndDateLessThan(CourseStatus.IN_PROGRESS, today);
        toFinish.forEach(Course::finish);

        log.info("코스 상태 전환: 시작 {}건, 종료 {}건", toStart.size(), toFinish.size());
    }
}
