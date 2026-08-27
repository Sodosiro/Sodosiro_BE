package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.controller.dto.CourseRecommendRequest;
import com.sodosiro.domain.course.controller.dto.CourseRecommendResponse;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.repository.CourseRepository;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.payload.code.error.CourseErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import com.sodosiro.global.service.RedisService;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.LongStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코스 추천 진입점. 1순위로 {@link CourseAiPlanner}에게 LLM 기반 코스 설계(스팟 구성 + 방문 순서)를 맡기고,
 * LLM 응답이 검증을 통과하지 못하면 {@link CourseRuleBasedPlanner}가 순수 Java 가중치 랜덤 선택으로 폴백한다.
 * 두 경로 모두 {@link CourseCandidatePoolBuilder}가 만든 같은 구조의 일자별 후보 풀(식당/스타일/랜덤,
 * 요일 휴무·전역 중복 이미 필터링됨)을 공유하므로, 하루 5슬롯(식당 2곳을 1·4번째 자리에 고정 배치 +
 * 선택 스타일별 1곳 + 남는 자리는 랜덤 풀) 구성 규칙이 두 경로에서 항상 동일하게 지켜진다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourseRecommendationService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final TouristSpotRepository touristSpotRepository;
    private final EmbeddingModel embeddingModel;
    private final CourseRepository courseRepository;
    private final CourseAiPlanner courseAiPlanner;
    private final CourseRuleBasedPlanner courseRuleBasedPlanner;
    private final RedisService redisService;

    @Value("${course.recommend.daily-limit:5}")
    private int dailyRecommendLimit;

    public CourseRecommendResponse recommend(Long userId, CourseRecommendRequest request) {

        validateDateRange(request.startDate(), request.endDate());
        validateSigunguCode(request.sigunguCode());
        validateNoOverlappingConfirmedTrip(userId, request.startDate(), request.endDate());
        consumeDailyRecommendationQuota(userId);
        List<LocalDate> dates = buildDateRange(request.startDate(), request.endDate());

        TouristSpot mustVisitSpot = request.mustVisitContentId() == null
                ? null
                : findTouristSpot(request.mustVisitContentId());

        int mustVisitDayIndex = mustVisitSpot == null ? -1 : resolveMustVisitDayIndex(mustVisitSpot, dates);

        // 비용발생: 사전 검증을 통과한 요청만 AI 임베딩 수행. AI 경로/규칙기반 경로가 같은 임베딩을 재사용하므로 한 번만 호출한다.
        float[] queryEmbedding = embedSafely(request.aiMessage());

        List<Course.DaySnapshot> days = courseAiPlanner
                .tryGenerate(request, dates, mustVisitSpot, mustVisitDayIndex, queryEmbedding)
                .orElseGet(() -> courseRuleBasedPlanner.generate(request, dates, mustVisitSpot, mustVisitDayIndex, queryEmbedding));

        Long courseId = saveDraft(userId, request, days);
        return new CourseRecommendResponse(courseId);
    }

    /** 사용자당 미확정 draft는 1개만 유지한다: 기존 미확정 draft가 있으면 지우고 새로 저장한다. */
    private Long saveDraft(Long userId, CourseRecommendRequest request, List<Course.DaySnapshot> days) {

        courseRepository.findByUserIdAndIsConfirmedFalse(userId).ifPresent(courseRepository::delete);

        Course draft = Course.createDraft(
                userId, request.title(), request.startDate(), request.endDate(), request.transportMode(),
                request.travelStylesOrEmpty(), request.mustVisitContentId(), request.aiMessage(), days);
        return courseRepository.save(draft).getId();
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new GeneralException(CourseErrorCode._INVALID_DATE_RANGE);
        }
    }

    /** sigunguCode는 tourist_spot.ldong_signgu_code(법정동 시군구 코드) 값이어야 한다. 옛 TourAPI sigunguCode 등 존재하지 않는 값이 오면 후보 풀이 조용히 비어버리므로 여기서 막는다. */
    private void validateSigunguCode(String sigunguCode) {
        if (!touristSpotRepository.existsByLdongSignguCode(sigunguCode)) {
            throw new GeneralException(CourseErrorCode._INVALID_SIGUNGU_CODE);
        }
    }

    /** 진행 중인(또는 예정된) 여행은 동시에 여러 개일 수 없으므로, 이미 확정된 다른 여행 기간과 겹치면 막는다. */
    private void validateNoOverlappingConfirmedTrip(Long userId, LocalDate startDate, LocalDate endDate) {
        boolean overlaps = courseRepository
                .existsByUserIdAndIsConfirmedTrueAndStatusNotAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                        userId, CourseStatus.FINISHED, endDate, startDate);
        if (overlaps) {
            throw new GeneralException(CourseErrorCode._TRAVEL_DATE_OVERLAP);
        }
    }

    /** 사용자당 하루(KST 자정 기준) 추천 생성 횟수를 제한한다. AI 호출(비용 발생) 이전에 원자적으로 소모한다. */
    private void consumeDailyRecommendationQuota(Long userId) {
        LocalDate today = LocalDate.now(KST);
        String key = "course:recommend:daily-count:%d:%s".formatted(userId, today);
        long ttlSeconds = Duration.between(LocalDateTime.now(KST), today.plusDays(1).atStartOfDay()).getSeconds();
        if (!redisService.tryConsumeDailyQuota(key, dailyRecommendLimit, ttlSeconds)) {
            throw new GeneralException(CourseErrorCode._DAILY_RECOMMEND_LIMIT_EXCEEDED);
        }
    }

    private List<LocalDate> buildDateRange(LocalDate startDate, LocalDate endDate) {
        long dayCount = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return LongStream.range(0, dayCount).mapToObj(startDate::plusDays).toList();
    }

    private TouristSpot findTouristSpot(Long contentId) {
        return touristSpotRepository.findById(contentId)
                .orElseThrow(() -> new GeneralException(CourseErrorCode._TOURIST_SPOT_NOT_FOUND));
    }

    /** mustVisit이 휴무가 아닌 첫 날짜에 배치한다. 여행 기간 내내 휴무면 예외를 던진다. */
    private int resolveMustVisitDayIndex(TouristSpot mustVisitSpot, List<LocalDate> dates) {
        for (int i = 0; i < dates.size(); i++) {
            String weekdayLabel = WeekdayLabels.labelOf(dates.get(i));
            if (WeekdayLabels.isOpenOn(mustVisitSpot, weekdayLabel)) {
                return i;
            }
        }
        // 모든 여행일자가 휴무일과 겹치면 에러처리
        throw new GeneralException(CourseErrorCode._MUST_VISIT_SPOT_ALWAYS_CLOSED);
    }

    /** AI 임베딩 호출은 외부 API 의존이라 실패할 수 있는데, 그래도 추천 자체는 계속 진행되어야 하므로 실패 시 임베딩 없이(null) 폴백한다. */
    private float[] embedSafely(String aiMessage) {
        if (aiMessage == null || aiMessage.isBlank()) {
            return null;
        }
        try {
            return embeddingModel.embed(aiMessage);
        } catch (RuntimeException exception) {
            log.warn("AI 한마디 임베딩 실패: aiMessage={}", aiMessage, exception);
            return null;
        }
    }
}
