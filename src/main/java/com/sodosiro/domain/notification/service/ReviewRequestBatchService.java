package com.sodosiro.domain.notification.service;

import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.notification.facade.NotificationFacade;
import com.sodosiro.domain.notification.factory.NotificationFactoryRegistry;
import com.sodosiro.domain.notification.repository.ReviewRequestQueryRepository;
import com.sodosiro.domain.notification.service.dto.ReviewRequestBatchResult;
import com.sodosiro.domain.notification.service.dto.ReviewRequestTarget;
import com.sodosiro.domain.review.repository.ReviewKey;
import com.sodosiro.domain.review.repository.ReviewRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 여행이 끝난 사용자에게 코스 단위로 리뷰 작성 유도 알림을 보내는 배치.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewRequestBatchService {

    private final ReviewRequestQueryRepository reviewRequestQueryRepository;
    private final ReviewRepository reviewRepository;
    private final NotificationFactoryRegistry notificationFactoryRegistry;
    private final NotificationFacade notificationFacade;

    @Value("${notification.review-request.after-days:0}")
    private long afterDays;

    @Value("${notification.review-request.grace-days:10}")
    private long graceDays;

    public ReviewRequestBatchResult run(String runId) {
        List<ReviewRequestTarget> targets = findTargets();

        int created = 0;
        for (ReviewRequestTarget target : targets) {
            created += createNotification(target);
        }

        ReviewRequestBatchResult result =
                new ReviewRequestBatchResult(targets.size(), created, targets.size() - created);
        log.info("리뷰 작성 유도 배치 완료 runId={} 대상={} 발송={} 쿨다운스킵={}",
                runId, result.targetedCourses(), result.createdNotifications(), result.skippedByCooldown());
        return result;
    }


    private int createNotification(ReviewRequestTarget target) {
        try {
            return notificationFactoryRegistry.create(target.toEvent(), notificationFacade);
        } catch (RuntimeException e) {
            log.warn("리뷰 작성 유도 알림 생성 실패 courseId={} userId={}",
                    target.courseId(), target.userId(), e);
            return 0;
        }
    }

    private List<ReviewRequestTarget> findTargets() {
        LocalDateTime now = LocalDateTime.now();
        List<Course> courses = reviewRequestQueryRepository.findReviewRequestTargets(
                now.minusDays(afterDays), now.minusDays(graceDays));
        if (courses.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = courses.stream()
                .map(Course::getUserId)
                .collect(Collectors.toUnmodifiableSet());
        Set<Long> contentIds = courses.stream()
                .flatMap(course -> course.allSpots().stream())
                .map(Course.SpotSnapshot::contentId)
                .collect(Collectors.toUnmodifiableSet());

        Set<ReviewKey> written = reviewRepository.findWrittenKeys(userIds, contentIds);

        return courses.stream()
                .map(course -> toTarget(course, written))
                .filter(target -> target.pendingSpotCount() > 0)
                .toList();
    }

    private ReviewRequestTarget toTarget(Course course, Set<ReviewKey> written) {
        int pendingSpotCount = (int) course.allSpots().stream()
                .map(Course.SpotSnapshot::contentId)
                .distinct()
                .filter(contentId -> !written.contains(new ReviewKey(course.getUserId(), contentId)))
                .count();

        return new ReviewRequestTarget(
                course.getUserId(),
                course.getId(),
                course.displayName(),
                pendingSpotCount
        );
    }
}
