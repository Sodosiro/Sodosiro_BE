package com.sodosiro.domain.notification.service;

import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.notification.facade.NotificationFacade;
import com.sodosiro.domain.notification.factory.NotificationFactoryRegistry;
import com.sodosiro.domain.notification.repository.CourseConfirmReminderQueryRepository;
import com.sodosiro.domain.notification.service.dto.CourseConfirmReminderBatchResult;
import com.sodosiro.domain.notification.service.dto.CourseConfirmReminderTarget;
import com.sodosiro.global.utils.TimeZones;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;


@Slf4j
@Service
@RequiredArgsConstructor
public class CourseConfirmReminderBatchService {

    private final CourseConfirmReminderQueryRepository courseConfirmReminderQueryRepository;
    private final NotificationFactoryRegistry notificationFactoryRegistry;
    private final NotificationFacade notificationFacade;

    @Value("${notification.course-confirm-reminder.before-days:1}")
    private long beforeDays;

    public CourseConfirmReminderBatchResult run(String runId) {
        List<CourseConfirmReminderTarget> targets = findTargets();

        int created = 0;
        for (CourseConfirmReminderTarget target : targets) {
            created += createNotification(target);
        }

        CourseConfirmReminderBatchResult result =
                new CourseConfirmReminderBatchResult(targets.size(), created, targets.size() - created);
        log.info("코스 확정 유도 배치 완료 runId={} 대상={} 발송={} 쿨다운스킵={}",
                runId, result.targetedCourses(), result.createdNotifications(), result.skippedByCooldown());
        return result;
    }

    private int createNotification(CourseConfirmReminderTarget target) {
        try {
            return notificationFactoryRegistry.create(target.toEvent(), notificationFacade);
        } catch (RuntimeException e) {
            log.warn("코스 확정 유도 알림 생성 실패 courseId={} userId={}",
                    target.courseId(), target.userId(), e);
            return 0;
        }
    }

    private List<CourseConfirmReminderTarget> findTargets() {
        LocalDate startDate = LocalDate.now(TimeZones.KST).plusDays(beforeDays);
        return courseConfirmReminderQueryRepository.findUnconfirmedCoursesStartingOn(startDate).stream()
                .map(course -> new CourseConfirmReminderTarget(
                        course.getUserId(), course.getId(), course.getStartDate()))
                .toList();
    }
}
