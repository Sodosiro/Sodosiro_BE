package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.repository.CourseRepository;
import com.sodosiro.global.utils.TimeZones;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


/**
 * KST 자정에 확정된 코스의 여행 상태를 오늘 날짜 기준으로 자동 전환한다 (UPCOMING→IN_PROGRESS, IN_PROGRESS→FINISHED).
 * 승격 시 근처 찜 알림이 참조하는 활성 코스 캐시를 기록하고, 종료 시 제거한다.
 *
 * <p>쿼리가 전환 대상 status만 잡으므로 여러 번 실행해도 안전하다. 앱 기동 시에도 같은 로직을 한 번 더 실행해,
 * 크론 실행 시각에 서버가 내려가 있었던 경우를 다음 재시작 때 스스로 채운다(self-healing).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CourseStatusScheduler {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private final CourseRepository courseRepository;
    private final ActiveCourseCacheWriter activeCourseCacheWriter;


    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void updateStatuses() {
        LocalDate today = LocalDate.now(TimeZones.KST);

        List<Course> toStart = courseRepository.findByIsConfirmedTrueAndStatusAndStartDateLessThanEqual(CourseStatus.UPCOMING, today);
        toStart.forEach(course -> {
            course.start();
            activeCourseCacheWriter.write(course.getUserId(), course);
        });

        List<Course> toFinish = courseRepository.findByIsConfirmedTrueAndStatusAndEndDateLessThan(CourseStatus.IN_PROGRESS, today);
        toFinish.forEach(course -> {
            course.finish();
            activeCourseCacheWriter.evict(course.getUserId());
        });

        log.info("코스 상태 전환: 시작 {}건, 종료 {}건", toStart.size(), toFinish.size());
    }
}
