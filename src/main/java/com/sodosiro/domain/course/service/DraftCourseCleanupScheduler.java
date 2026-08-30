package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.repository.CourseRepository;
import com.sodosiro.global.utils.TimeZones;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * KST 자정에 출발일이 지난 미확정(draft) 코스를 자동 삭제한다.
 * 사용자가 확정하지 않은 채 방치한 AI 추천 결과는 출발일이 지나면 더 이상 의미가 없어 정리 대상이다.
 *
 * <p>쿼리가 조건에 맞는 row만 지우므로 여러 번 실행해도 안전하다. 앱 기동 시에도 같은 로직을 한 번 더 실행해,
 * 크론 실행 시각에 서버가 내려가 있었던 경우를 다음 재시작 때 스스로 채운다(self-healing).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DraftCourseCleanupScheduler {

    private final CourseRepository courseRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    @Transactional
    public void deleteExpiredDrafts() {
        LocalDate today = LocalDate.now(TimeZones.KST);

        long deletedCount = courseRepository.deleteAllByIsConfirmedFalseAndStartDateBefore(today);

        log.info("만료된 임시 저장 코스 삭제: {}건", deletedCount);
    }
}
