package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.controller.dto.CourseRecommendRequest;
import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.course.service.dto.CandidatePoolResult;
import com.sodosiro.domain.course.service.dto.CandidateSpot;
import com.sodosiro.domain.course.service.dto.DayCandidatePool;
import com.sodosiro.domain.course.service.dto.DaySlotNeeds;
import com.sodosiro.domain.travel.entity.TouristSpot;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.springframework.stereotype.Service;

/**
 * {@link CourseAiPlanner}와 같은 {@link CourseCandidatePoolBuilder} 후보 풀을 쓰지만, LLM 대신 순수 Java
 * 가중치 랜덤으로 그날의 스팟을 고른다. 각 버킷(식당/스타일/랜덤)에서 {@link DaySlotNeeds}가 요구하는 개수만큼
 * {@link #pickWeighted}로 뽑는데, 이 경로는 AI 경로와 달리 "부족하면 실패" 대신 "부족하면 있는 만큼만" 정책을 쓴다
 * (재시도 없이 반드시 코스를 반환해야 하는 최종 폴백이므로). 그래서 후보가 모자란 날은 5곳/식당 2곳이라는
 * 계약이 깨질 수 있다 — 하루 스팟 수가 5보다 적거나 식당이 0~1곳일 수 있다. 뽑은 뒤에는 mustVisit(있다면)을
 * 기준으로 동선(위경도) 근접순 정렬을 거쳐, 마지막으로 {@link MealSlotOrdering}이 있는 식당만큼만
 * 점심·저녁 고정 자리로 옮긴다.
 */
@Service
class CourseRuleBasedPlanner {

    private final CourseCandidatePoolBuilder candidatePoolBuilder;
    private final Random random = new Random();

    CourseRuleBasedPlanner(CourseCandidatePoolBuilder candidatePoolBuilder) {
        this.candidatePoolBuilder = candidatePoolBuilder;
    }

    List<Course.DaySnapshot> generate(CourseRecommendRequest request, List<LocalDate> dates, TouristSpot mustVisitSpot, int mustVisitDayIndex, float[] queryEmbedding) {

        CandidatePoolResult pool = candidatePoolBuilder.build(request, dates, queryEmbedding, mustVisitSpot, mustVisitDayIndex);

        List<Course.DaySnapshot> days = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            DayCandidatePool dayPool = pool.dayPools().get(i);
            DaySlotNeeds needs = pool.slotNeeds().get(i);
            boolean mustVisitHere = i == mustVisitDayIndex;

            List<Long> contentIds = new ArrayList<>();
            if (mustVisitHere) {
                contentIds.add(mustVisitSpot.getContentId());
            }
            contentIds.addAll(idsOf(pickWeighted(dayPool.restaurants(), needs.restaurantNeeded())));
            for (Map.Entry<Integer, Integer> entry : needs.styleNeeded().entrySet()) {
                contentIds.addAll(idsOf(pickWeighted(dayPool.styleCandidates().getOrDefault(entry.getKey(), List.of()), entry.getValue())));
            }
            contentIds.addAll(idsOf(pickWeighted(dayPool.randomPool(), needs.randomNeeded())));

            List<TouristSpot> spotsForDay = contentIds.stream().map(pool.byId()::get).toList();
            if (spotsForDay.isEmpty()) {
                days.add(new Course.DaySnapshot(i + 1, dates.get(i), List.of()));
                continue;
            }
            List<TouristSpot> geoOrdered = orderByProximity(spotsForDay, mustVisitHere ? mustVisitSpot : null);
            List<Long> finalOrder = MealSlotOrdering.placeRestaurantsAtMealSlots(
                    geoOrdered.stream().map(TouristSpot::getContentId).toList(), pool.byId());

            List<Course.SpotSnapshot> spots = finalOrder.stream()
                    .map(contentId -> toSpotSnapshot(pool.byId().get(contentId), mustVisitHere && contentId.equals(mustVisitSpot.getContentId())))
                    .toList();
            days.add(new Course.DaySnapshot(i + 1, dates.get(i), spots));
        }
        return days;
    }

    private List<Long> idsOf(List<CandidateSpot> candidates) {
        return candidates.stream().map(CandidateSpot::id).toList();
    }

    /**
     * 후보 윈도우(이미 needed+여유분 크기로 좁혀져 있음)에서 가중치 랜덤으로 최대 count개를 중복 없이 뽑는다.
     * 순번(rank)이 앞설수록, 평점이 높을수록 더 자주 뽑힌다. 후보가 count보다 적으면(그 카테고리의 관광지가
     * 부족하면) 예외 없이 있는 만큼만 반환한다 — 이 경로는 반드시 코스를 만들어내야 하는 최종 폴백이라,
     * "완벽한 5곳"보다 "부족하더라도 있는 만큼"을 우선한다.
     */
    private List<CandidateSpot> pickWeighted(List<CandidateSpot> window, int count) {
        int actualCount = Math.min(count, window.size());
        if (actualCount <= 0) {
            return List.of();
        }
        List<CandidateSpot> remaining = new ArrayList<>(window);
        List<CandidateSpot> picked = new ArrayList<>(actualCount);
        for (int i = 0; i < actualCount; i++) {
            CandidateSpot chosen = pickOneWeighted(remaining);
            picked.add(chosen);
            remaining.remove(chosen);
        }
        return picked;
    }

    private CandidateSpot pickOneWeighted(List<CandidateSpot> candidates) {
        double[] weights = new double[candidates.size()];
        double totalWeight = 0;
        for (int i = 0; i < candidates.size(); i++) {
            BigDecimal rating = candidates.get(i).avgRating();
            weights[i] = (candidates.size() - i) + (rating == null ? 0.0 : rating.doubleValue());
            totalWeight += weights[i];
        }

        double target = random.nextDouble() * totalWeight;
        double cumulative = 0;
        for (int i = 0; i < candidates.size(); i++) {
            cumulative += weights[i];
            if (target < cumulative) {
                return candidates.get(i);
            }
        }
        return candidates.getLast();
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

    private Course.SpotSnapshot toSpotSnapshot(TouristSpot spot, boolean mustVisit) {
        return new Course.SpotSnapshot(
                spot.getContentId(), spot.getTitle(), spot.getFirstImage(),
                spot.getMapX(), spot.getMapY(), spot.getCategory(), mustVisit);
    }
}
