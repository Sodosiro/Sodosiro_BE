package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.constants.TravelStyle;
import com.sodosiro.domain.course.controller.dto.CourseRecommendRequest;
import com.sodosiro.domain.course.controller.dto.CourseRecommendResponse;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.repository.CourseRepository;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.SpotEmbeddingRepository;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.payload.code.error.CourseErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코스 추천 진입점. 1순위로 {@link CourseAiPlanner}에게 실제 코스 설계(스팟 구성 + 방문 순서)를 맡기고,
 * LLM 응답이 검증을 통과하지 못하면 규칙기반 알고리즘(임베딩/평점 가중치 랜덤 + 그리디 동선 정렬)으로 폴백한다.
 * 하루 5개 슬롯 = 필수 슬롯 3개(식당 1, 카페 1, 관광/자연/액티비티/쇼핑 1) + 자율 슬롯 2개(후보 풀 상위에서 가중치 랜덤)로 구성하고,
 * 요일 휴무는 전 슬롯에서 제외하며, 같은 날은 동선(위경도) 근접순으로 정렬한다.
 * 필수 슬롯은 그룹별로 카테고리를 하드 필터링(WHERE)한 전용 풀에서 뽑아 카테고리 정확성을 보장하고,
 * 자율 슬롯은 카테고리 제한 없는 공용 풀에서 뽑아 선택하지 않은 카테고리도 노출될 수 있게 한다.
 * 슬롯은 후보 풀 상위 {@value #RANDOM_POOL_SIZE}개 안에서 가중치 랜덤으로 뽑으므로,
 * 같은 조건으로 다시 호출해도 매번 다른 조합이 나오되 aiMessage 매칭·선택 카테고리·평점이 높을수록 더 자주 뽑힌다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CourseRecommendationService {

    private static final int FREE_SLOTS_PER_DAY = 2;
    private static final int EMBEDDING_POOL_LIMIT = 60;
    private static final int REQUIRED_SLOT_POOL_LIMIT = 200;
    private static final int RANDOM_POOL_SIZE = 12;

    private final TouristSpotRepository touristSpotRepository;
    private final SpotEmbeddingRepository spotEmbeddingRepository;
    private final EmbeddingModel embeddingModel;
    private final CourseRepository courseRepository;
    private final CourseAiPlanner courseAiPlanner;
    private final Random random = new Random();

    public CourseRecommendResponse recommend(Long userId, CourseRecommendRequest request) {

        validateDateRange(request.startDate(), request.endDate());
        List<LocalDate> dates = buildDateRange(request.startDate(), request.endDate());

        TouristSpot mustVisitSpot = request.mustVisitContentId() == null
                ? null
                : findTouristSpot(request.mustVisitContentId());

        // AI 임베딩 호출 전에 필수 방문지 검증
        Set<Long> usedContentIds = new HashSet<>();

        // 필수 방문지 지정 및 userContentIds에 등록 (중복 방지)
        if (mustVisitSpot != null) {
            usedContentIds.add(mustVisitSpot.getContentId());
        }
        int mustVisitDayIndex = mustVisitSpot == null ? -1 : resolveMustVisitDayIndex(mustVisitSpot, dates);

        // 비용발생: 사전 검증을 통과한 요청 AI 임베딩 수행. AI 경로/규칙기반 폴백이 같은 임베딩을 재사용하므로 한 번만 호출한다.
        List<Integer> categoryCodes = request.travelStylesOrEmpty().stream()
                .map(TravelStyle::categoryCode)
                .toList();
        float[] queryEmbedding = embedSafely(request.aiMessage());

        List<CourseRecommendResponse.DayCourse> days = courseAiPlanner
                .tryGenerate(request, dates, mustVisitSpot, queryEmbedding)
                .orElseGet(() -> generateWithRuleBasedFallback(
                        request, dates, mustVisitSpot, mustVisitDayIndex, usedContentIds, categoryCodes, queryEmbedding));

        Long courseId = saveDraft(userId, request, days);
        return new CourseRecommendResponse(courseId, days);
    }

    /** 사용자당 미확정 draft는 1개만 유지한다: 기존 미확정 draft가 있으면 지우고 새로 저장한다. */
    private Long saveDraft(Long userId, CourseRecommendRequest request, List<CourseRecommendResponse.DayCourse> days) {
        courseRepository.findByUserIdAndIsConfirmedFalse(userId).ifPresent(courseRepository::delete);
        List<Course.DaySnapshot> snapshots = days.stream().map(this::toSnapshot).toList();
        Course draft = Course.createDraft(
                userId, request.startDate(), request.endDate(),
                request.travelStylesOrEmpty(), request.mustVisitContentId(), request.aiMessage(), snapshots);
        return courseRepository.save(draft).getId();
    }

    private Course.DaySnapshot toSnapshot(CourseRecommendResponse.DayCourse day) {
        List<Course.SpotSnapshot> spots = day.spots().stream()
                .map(spot -> new Course.SpotSnapshot(
                        spot.contentId(), spot.title(), spot.firstImage(),
                        spot.mapX(), spot.mapY(), spot.category(), spot.mustVisit()))
                .toList();
        return new Course.DaySnapshot(day.day(), day.date(), spots);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new GeneralException(CourseErrorCode._INVALID_DATE_RANGE);
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

    /** mustVisit이 휴무가 아닌 첫 날짜에 배치한다(규칙기반 폴백에서만 이 인덱스로 고정 배치; AI 경로는 직접 날짜를 고른다). 여행 기간 내내 휴무면 예외를 던진다. */
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

    // =====================================================================================
    // 규칙기반 폴백 (CourseAiPlanner 검증 실패 시에만 사용)
    // =====================================================================================

    private List<CourseRecommendResponse.DayCourse> generateWithRuleBasedFallback(
            CourseRecommendRequest request, List<LocalDate> dates, TouristSpot mustVisitSpot, int mustVisitDayIndex,
            Set<Long> usedContentIds, List<Integer> categoryCodes, float[] queryEmbedding) {

        List<TouristSpot> freeSlotPool = buildFreeSlotPool(categoryCodes, request.sigunguCode(), queryEmbedding);
        Map<RequiredSlotGroup, List<TouristSpot>> requiredSlotPools =
                buildRequiredSlotPools(categoryCodes, request.sigunguCode(), queryEmbedding);

        List<CourseRecommendResponse.DayCourse> days = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = dates.get(i);
            String weekdayLabel = WeekdayLabels.labelOf(date);
            boolean assignMustVisit = i == mustVisitDayIndex;
            RequiredSlotGroup satisfiedGroup = assignMustVisit
                    ? RequiredSlotGroup.groupOf(mustVisitSpot.getCategory()).orElse(null)
                    : null;

            //(ai>category>인기순) 뽑고 제외
            List<TouristSpot> picked = pickForDay(
                    requiredSlotPools, freeSlotPool, usedContentIds, weekdayLabel, assignMustVisit, satisfiedGroup);
            usedContentIds.addAll(picked.stream().map(TouristSpot::getContentId).toList());

            List<TouristSpot> daySpots = new ArrayList<>(picked);
            if (assignMustVisit) {
                daySpots.add(0, mustVisitSpot);
            }
            List<TouristSpot> ordered = orderByProximity(daySpots, assignMustVisit ? mustVisitSpot : null);

            List<CourseRecommendResponse.RecommendedSpot> spots = ordered.stream()
                    .map(spot -> toRecommendedSpot(
                            spot, assignMustVisit && spot.getContentId().equals(mustVisitSpot.getContentId())))
                    .toList();
            days.add(new CourseRecommendResponse.DayCourse(i + 1, date, spots));
        }
        return days;
    }

    /**
     * 자율 슬롯 전용 풀: 임베딩 유사도 후보(카테고리 선택 시 해당 카테고리를 가중치로 우선 배치) + 카테고리 후보 + 인기순 폴백 후보를
     * 이 순서로 합쳐(중복 제거) 하나의 풀로 만든다. 카테고리로 걸러내지 않으므로 선택하지 않은 카테고리도 노출될 수 있다.
     */
    private List<TouristSpot> buildFreeSlotPool(List<Integer> categoryCodes, String sigunguCode, float[] queryEmbedding) {

        List<TouristSpot> embeddingCandidates = queryEmbedding == null
                ? List.of()
                : findEmbeddingCandidatesSafely(queryEmbedding, categoryCodes, sigunguCode);

        // 없으면 - 빈리스트, 있다면 - 해당 카테고리의 평점높은순 200개
        List<TouristSpot> categoryCandidates = categoryCodes.isEmpty()
                ? List.of()
                : touristSpotRepository.findByCategoryInAndSigunguCodeOrderByAvgRatingDesc(
                        categoryCodes, sigunguCode, PageRequest.of(0, REQUIRED_SLOT_POOL_LIMIT));

        List<TouristSpot> fallbackCandidates =
                touristSpotRepository.findBySigunguCodeOrderByAvgRatingDesc(sigunguCode, PageRequest.of(0, REQUIRED_SLOT_POOL_LIMIT));

        LinkedHashMap<Long, TouristSpot> merged = new LinkedHashMap<>();
        for (TouristSpot spot : embeddingCandidates) {
            merged.putIfAbsent(spot.getContentId(), spot);
        }
        for (TouristSpot spot : categoryCandidates) {
            merged.putIfAbsent(spot.getContentId(), spot);
        }
        for (TouristSpot spot : fallbackCandidates) {
            merged.putIfAbsent(spot.getContentId(), spot);
        }
        return List.copyOf(merged.values());
    }

    /** 필수 슬롯 그룹별로 카테고리를 하드 필터링한 전용 풀을 만든다. */
    private Map<RequiredSlotGroup, List<TouristSpot>> buildRequiredSlotPools(
            List<Integer> categoryCodes, String sigunguCode, float[] queryEmbedding) {
        Map<RequiredSlotGroup, List<TouristSpot>> pools = new EnumMap<>(RequiredSlotGroup.class);
        for (RequiredSlotGroup group : RequiredSlotGroup.values()) {
            pools.put(group, buildRequiredSlotPool(group, categoryCodes, sigunguCode, queryEmbedding));
        }
        return pools;
    }

    /**
     * group.categoryCodes로 하드 필터링(WHERE)한 뒤 그 안에서 AI 유사도로 정렬한 후보 +
     * 같은 카테고리 범위의 평점순 폴백(선택 스타일 우선 배치)을 합친다. 카테고리는 쿼리 단에서 이미 보장되므로
     * 별도의 카테고리 필터링/폴백 조회가 필요 없다.
     */
    private List<TouristSpot> buildRequiredSlotPool(
            RequiredSlotGroup group, List<Integer> categoryCodes, String sigunguCode, float[] queryEmbedding) {

        List<TouristSpot> embeddingCandidates = queryEmbedding == null
                ? List.of()
                : findRequiredSlotEmbeddingCandidatesSafely(group, queryEmbedding, categoryCodes, sigunguCode);

        List<TouristSpot> fallbackCandidates = orderByStyleBoost(
                touristSpotRepository.findByCategoryInAndSigunguCodeOrderByAvgRatingDesc(
                        group.categoryCodes, sigunguCode, PageRequest.of(0, REQUIRED_SLOT_POOL_LIMIT)),
                categoryCodes);

        LinkedHashMap<Long, TouristSpot> merged = new LinkedHashMap<>();
        for (TouristSpot spot : embeddingCandidates) {
            merged.putIfAbsent(spot.getContentId(), spot);
        }
        for (TouristSpot spot : fallbackCandidates) {
            merged.putIfAbsent(spot.getContentId(), spot);
        }
        return List.copyOf(merged.values());
    }

    /** 평점순 리스트를 상대 순서는 유지한 채 선택 스타일(categoryCodes) 일치 항목을 앞쪽으로 재배치한다. */
    private List<TouristSpot> orderByStyleBoost(List<TouristSpot> spots, List<Integer> categoryCodes) {
        if (categoryCodes.isEmpty()) {
            return spots;
        }
        Map<Boolean, List<TouristSpot>> partitioned = spots.stream()
                .collect(Collectors.partitioningBy(spot -> categoryCodes.contains(spot.getCategory())));
        List<TouristSpot> ordered = new ArrayList<>(partitioned.get(true));
        ordered.addAll(partitioned.get(false));
        return ordered;
    }

    private List<TouristSpot> findEmbeddingCandidatesSafely(
            float[] queryEmbedding, List<Integer> categoryCodes, String sigunguCode) {
        try {
            List<Long> nearestIds = spotEmbeddingRepository
                    .findNearestContentIds(queryEmbedding, categoryCodes, sigunguCode, EMBEDDING_POOL_LIMIT);
            return resolveSpots(nearestIds);
        } catch (RuntimeException exception) {
            log.warn("AI 한마디 임베딩 후보 조회 실패", exception);
            return List.of();
        }
    }

    private List<TouristSpot> findRequiredSlotEmbeddingCandidatesSafely(
            RequiredSlotGroup group, float[] queryEmbedding, List<Integer> categoryCodes, String sigunguCode) {
        try {
            List<Long> nearestIds = spotEmbeddingRepository.findNearestContentIdsInCategories(
                    queryEmbedding, group.categoryCodes, categoryCodes, sigunguCode, REQUIRED_SLOT_POOL_LIMIT);
            return resolveSpots(nearestIds);
        } catch (RuntimeException exception) {
            log.warn("필수슬롯({}) 임베딩 후보 조회 실패", group, exception);
            return List.of();
        }
    }

    private List<TouristSpot> resolveSpots(List<Long> contentIds) {
        Map<Long, TouristSpot> spotsById = touristSpotRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));
        return contentIds.stream().map(spotsById::get).filter(Objects::nonNull).toList();
    }

    /**
     * 필수 슬롯(satisfiedGroup으로 이미 채워진 그룹은 제외) 각 1개 + 자율 슬롯을 채운다.
     * mustVisit이 배정되는 날은 mustVisit이 5개 슬롯 중 하나를 차지하므로(추가 슬롯이 아님),
     * mustVisit의 카테고리가 어떤 필수 그룹에도 속하지 않으면(satisfiedGroup == null) 자율 슬롯을 하나 줄인다.
     * 자율 슬롯은 공용 풀 상위 순서를 그대로 따르므로 여행스타일/AI 한마디 가중치가 자연스럽게 반영된다.
     */
    private List<TouristSpot> pickForDay(
            Map<RequiredSlotGroup, List<TouristSpot>> requiredSlotPools, List<TouristSpot> freeSlotPool,
            Set<Long> usedContentIds, String weekdayLabel, boolean assignMustVisit, RequiredSlotGroup satisfiedGroup) {

        Set<Long> excluded = new HashSet<>(usedContentIds);
        List<TouristSpot> picked = new ArrayList<>();

        for (RequiredSlotGroup group : RequiredSlotGroup.values()) {
            if (group == satisfiedGroup) {
                continue;
            }
            TouristSpot spot = pickForGroup(requiredSlotPools.get(group), excluded, weekdayLabel)
                    .orElseThrow(() -> new GeneralException(CourseErrorCode._REQUIRED_SLOT_INSUFFICIENT));
            picked.add(spot);
            excluded.add(spot.getContentId());
        }

        int freeSlotsToday = FREE_SLOTS_PER_DAY;
        if (assignMustVisit && satisfiedGroup == null) {
            freeSlotsToday -= 1;
        }
        picked.addAll(pickFreeSlots(freeSlotPool, excluded, weekdayLabel, freeSlotsToday));
        return picked;
    }

    /** 그룹 전용 풀은 쿼리 단에서 이미 카테고리가 보장되므로, 제외 목록/휴무만 걸러 가중치 랜덤으로 뽑는다. */
    private Optional<TouristSpot> pickForGroup(List<TouristSpot> groupPool, Set<Long> excluded, String weekdayLabel) {
        List<TouristSpot> eligible = groupPool.stream()
                .filter(spot -> !excluded.contains(spot.getContentId()) && WeekdayLabels.isOpenOn(spot, weekdayLabel))
                .toList();
        return pickWeighted(eligible);
    }

    private List<TouristSpot> pickFreeSlots(
            List<TouristSpot> pool, Set<Long> excluded, String weekdayLabel, int quota) {
        Set<Long> picking = new HashSet<>(excluded);
        List<TouristSpot> picked = new ArrayList<>(quota);
        for (int i = 0; i < quota; i++) {
            List<TouristSpot> eligible = pool.stream()
                    .filter(spot -> !picking.contains(spot.getContentId()) && WeekdayLabels.isOpenOn(spot, weekdayLabel))
                    .toList();
            Optional<TouristSpot> chosen = pickWeighted(eligible);
            if (chosen.isEmpty()) {
                break;
            }
            picked.add(chosen.get());
            picking.add(chosen.get().getContentId());
        }
        if (picked.size() < quota) {
            throw new GeneralException(CourseErrorCode._FREE_SLOT_INSUFFICIENT);
        }
        return picked;
    }

    /**
     * 후보를 상위 {@value #RANDOM_POOL_SIZE}개로 좁힌 뒤 가중치 랜덤으로 하나를 뽑는다.
     * 풀 안 순번(rank)이 곧 aiMessage 임베딩 매칭 → 선택 카테고리 매칭 → 인기 폴백 순서를 반영하므로,
     * 순번이 앞설수록 가중치가 커지고(최대 {@value #RANDOM_POOL_SIZE}) 평점은 그 안에서 보조 가중치로만 더해진다.
     */
    private Optional<TouristSpot> pickWeighted(List<TouristSpot> eligible) {
        if (eligible.isEmpty()) {
            return Optional.empty();
        }
        List<TouristSpot> narrowed = eligible.size() > RANDOM_POOL_SIZE
                ? eligible.subList(0, RANDOM_POOL_SIZE)
                : eligible;

        double[] weights = new double[narrowed.size()];
        double totalWeight = 0;
        for (int i = 0; i < narrowed.size(); i++) {
            BigDecimal rating = narrowed.get(i).getAvgRating();
            weights[i] = (narrowed.size() - i) + (rating == null ? 0.0 : rating.doubleValue());
            totalWeight += weights[i];
        }

        double target = random.nextDouble() * totalWeight;
        double cumulative = 0;
        for (int i = 0; i < narrowed.size(); i++) {
            cumulative += weights[i];
            if (target < cumulative) {
                return Optional.of(narrowed.get(i));
            }
        }
        return Optional.of(narrowed.get(narrowed.size() - 1));
    }

    /** 필수 슬롯 그룹: 식당(1), 카페(2), 관광/자연/액티비티/쇼핑(3~6). 숙박(7)은 어느 그룹에도 속하지 않는다. */
    private enum RequiredSlotGroup {
        RESTAURANT(List.of(TravelStyle.RESTAURANT.categoryCode())),
        CAFE(List.of(TravelStyle.CAFE.categoryCode())),
        EXPERIENCE(List.of(
                TravelStyle.TOURIST_SPOT.categoryCode(),
                TravelStyle.NATURE.categoryCode(),
                TravelStyle.ACTIVITY.categoryCode(),
                TravelStyle.SHOPPING.categoryCode()));

        private final List<Integer> categoryCodes;

        RequiredSlotGroup(List<Integer> categoryCodes) {
            this.categoryCodes = categoryCodes;
        }

        boolean matches(Integer category) {
            return category != null && categoryCodes.contains(category);
        }

        static Optional<RequiredSlotGroup> groupOf(Integer category) {
            return Arrays.stream(values()).filter(group -> group.matches(category)).findFirst();
        }
    }

    /** anchor(있으면 mustVisit)에서 시작해 가장 가까운 지점을 그리디하게 이어 붙인 동선 순서를 만든다. */
    private List<TouristSpot> orderByProximity(List<TouristSpot> spots, TouristSpot anchor) {
        List<TouristSpot> remaining = new ArrayList<>(spots);
        TouristSpot current = anchor != null ? anchor : remaining.removeFirst();
        if (anchor != null) {
            remaining.remove(anchor);
        }

        List<TouristSpot> ordered = new ArrayList<>(spots.size());
        ordered.add(current);
        while (!remaining.isEmpty()) {
            TouristSpot from = current;
            TouristSpot nearest = remaining.stream()
                    .min(Comparator.comparingDouble(spot -> squaredDistance(from, spot)))
                    .orElseThrow();
            remaining.remove(nearest);
            ordered.add(nearest);
            current = nearest;
        }
        return ordered;
    }

    private double squaredDistance(TouristSpot a, TouristSpot b) {
        if (a.getMapX() == null || a.getMapY() == null || b.getMapX() == null || b.getMapY() == null) {
            return Double.MAX_VALUE;
        }
        double dx = a.getMapX().doubleValue() - b.getMapX().doubleValue();
        double dy = a.getMapY().doubleValue() - b.getMapY().doubleValue();
        return dx * dx + dy * dy;
    }

    private CourseRecommendResponse.RecommendedSpot toRecommendedSpot(TouristSpot spot, boolean mustVisit) {
        return new CourseRecommendResponse.RecommendedSpot(
                spot.getContentId(), spot.getTitle(), spot.getFirstImage(),
                spot.getMapX(), spot.getMapY(), spot.getCategory(), mustVisit);
    }
}
