package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.constants.TravelStyle;
import com.sodosiro.domain.course.controller.dto.CourseRecommendRequest;
import com.sodosiro.domain.course.service.dto.CandidatePoolResult;
import com.sodosiro.domain.course.service.dto.CandidateSpot;
import com.sodosiro.domain.course.service.dto.DayCandidatePool;
import com.sodosiro.domain.course.service.dto.DaySlotNeeds;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.SpotEmbeddingRepository;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 일자별로 격리된 후보 풀(Day-specific Pool)을 구성한다. {@link CourseAiPlanner}(LLM 선택)와
 * {@link CourseRuleBasedPlanner}(가중치 랜덤 선택)가 이 후보 풀을 공유하므로, "어떤 후보를 보여줄지"는
 * 여기서 한 곳에서만 결정하고 두 플래너는 "그 후보 중 무엇을 고를지"만 서로 다르게 구현한다.
 * 여행 전체 기간을 관통하는 전역 중복 방지 장바구니({@code globalUsedIds})를 두고, 날짜를 순회하며 마스터 풀에서
 * 그날 필요한 개수보다 조금 넉넉하게(여유분 {@value #CANDIDATE_WINDOW_EXTRA}) 뽑아 즉시 잠그기 때문에,
 * 이후 날짜의 풀에는 애초에 등장하지 않는다. 휴무일도 그날의 풀을 만드는 시점에 걸러낸다.
 * mustVisit이 배정될 날짜는 호출자(CourseRecommendationService)가 사전에 확정해 넘겨준다.
 */
@Component
class CourseCandidatePoolBuilder {

    static final int DAILY_SLOT_COUNT = 5;
    static final int RESTAURANT_SLOTS_PER_DAY = 2;
    private static final int CANDIDATE_BUFFER = 3;
    private static final int CANDIDATE_LIMIT_CAP = 200;
    private static final int CANDIDATE_WINDOW_EXTRA = 2;

    private final TouristSpotRepository touristSpotRepository;
    private final SpotEmbeddingRepository spotEmbeddingRepository;

    CourseCandidatePoolBuilder(TouristSpotRepository touristSpotRepository, SpotEmbeddingRepository spotEmbeddingRepository) {
        this.touristSpotRepository = touristSpotRepository;
        this.spotEmbeddingRepository = spotEmbeddingRepository;
    }

    CandidatePoolResult build(CourseRecommendRequest request, List<LocalDate> dates, float[] queryEmbedding, TouristSpot mustVisitSpot, int mustVisitDayIndex) {

        int tripDays = dates.size();

        // 식당 리스트로 감싸서 넉넉하게 뽑기
        List<Integer> restaurantCategory = List.of(TravelStyle.RESTAURANT.categoryCode());
        List<TouristSpot> restaurantMaster = fetchByCategory(restaurantCategory, request.sigunguCode(), queryEmbedding, poolLimit(RESTAURANT_SLOTS_PER_DAY, tripDays));

        //사용자가 선택한 카테고리 추출
        List<Integer> styleCodes = request.travelStylesOrEmpty().stream()
                .filter(style -> style != TravelStyle.RESTAURANT)
                .map(TravelStyle::categoryCode)
                .toList();

        //마스터 풀: 여행 전체 기간에서 소비할 양을 감안해 넉넉하게(CANDIDATE_BUFFER) 한 번만 조회한다.
        Map<Integer, List<TouristSpot>> styleMaster = new LinkedHashMap<>();
        for (Integer code : styleCodes) {
            styleMaster.put(code, fetchByCategory(List.of(code), request.sigunguCode(), queryEmbedding, poolLimit(1, tripDays)));
        }

        List<Integer> nonRestaurantCategories = Arrays.stream(TravelStyle.values())
                .filter(style -> style != TravelStyle.RESTAURANT)
                .map(TravelStyle::categoryCode)
                .toList();

        int baseRandomSlots = Math.max(1, DAILY_SLOT_COUNT - RESTAURANT_SLOTS_PER_DAY - styleCodes.size());
        List<TouristSpot> randomMaster = fetchByCategory(nonRestaurantCategories, request.sigunguCode(), queryEmbedding, poolLimit(baseRandomSlots, tripDays));

        //전역 중복 방지 장바구니: 필수 방문지가 있다면 미리 담아 다른 날짜 풀에 다시 등장하지 않게 한다.
        Set<Long> globalUsedIds = new HashSet<>();
        Map<Long, TouristSpot> byId = new LinkedHashMap<>();
        if (mustVisitSpot != null) {
            globalUsedIds.add(mustVisitSpot.getContentId());
            byId.put(mustVisitSpot.getContentId(), mustVisitSpot);
        }

        List<DayCandidatePool> dayPools = new ArrayList<>();
        List<DaySlotNeeds> slotNeeds = new ArrayList<>();

        for (int i = 0; i < tripDays; i++) {
            LocalDate date = dates.get(i);
            String weekday = WeekdayLabels.labelOf(date);
            boolean mustVisitHere = i == mustVisitDayIndex;
            Integer mustVisitCategory = mustVisitHere ? mustVisitSpot.getCategory() : null;
            boolean mustVisitIsRestaurant = mustVisitHere && Objects.equals(mustVisitCategory, TravelStyle.RESTAURANT.categoryCode());
            boolean mustVisitMatchesStyle = mustVisitHere && styleCodes.contains(mustVisitCategory);

            //1단계: 그 요일에 영업하고 아직 안 쓰인 식당을 필요 수만큼(+여유분) 뽑아 즉시 잠근다.
            int restaurantNeeded = RESTAURANT_SLOTS_PER_DAY - (mustVisitIsRestaurant ? 1 : 0);
            List<TouristSpot> restaurantsForDay = takeForDay(restaurantMaster, globalUsedIds, weekday, windowSize(restaurantNeeded));
            byId.putAll(toMap(restaurantsForDay));

            //2단계: 취향별 스팟을 1개씩(mustVisit이 그 취향을 이미 채웠다면 생략) 뽑아 잠근다.
            Map<Integer, List<CandidateSpot>> styleForDay = new LinkedHashMap<>();
            Map<Integer, Integer> styleNeededForDay = new LinkedHashMap<>();
            for (Integer code : styleCodes) {
                int needed = (mustVisitHere && code.equals(mustVisitCategory)) ? 0 : 1;
                List<TouristSpot> picked = takeForDay(styleMaster.get(code), globalUsedIds, weekday, windowSize(needed));
                byId.putAll(toMap(picked));
                styleForDay.put(code, picked.stream().map(CandidateSpot::from).toList());
                styleNeededForDay.put(code, needed);
            }

            //3단계: 남은 슬롯만큼 식당 외 전체 카테고리에서 랜덤 풀을 채운다.
            int randomNeeded = Math.max(0, DAILY_SLOT_COUNT - RESTAURANT_SLOTS_PER_DAY - styleCodes.size() - (mustVisitHere && !mustVisitIsRestaurant && !mustVisitMatchesStyle ? 1 : 0));
            List<TouristSpot> randomForDay = takeForDay(randomMaster, globalUsedIds, weekday, windowSize(randomNeeded));
            byId.putAll(toMap(randomForDay));

            dayPools.add(new DayCandidatePool(
                    i + 1, date.toString(), weekday,
                    restaurantsForDay.stream().map(CandidateSpot::from).toList(),
                    styleForDay,
                    randomForDay.stream().map(CandidateSpot::from).toList(),
                    mustVisitHere ? CandidateSpot.from(mustVisitSpot) : null));
            slotNeeds.add(new DaySlotNeeds(restaurantNeeded, styleNeededForDay, randomNeeded));
        }

        return new CandidatePoolResult(dayPools, slotNeeds, byId);
    }

    /** 마스터 풀에서 그 요일에 영업하고 아직 잠기지 않은 것부터 순서대로 window개를 뽑고, 뽑힌 전부(미선택분 포함)를 즉시 잠근다. */
    private List<TouristSpot> takeForDay(List<TouristSpot> master, Set<Long> globalUsedIds, String weekday, int window) {
        if (window <= 0 || master == null) {
            return List.of();
        }
        List<TouristSpot> taken = new ArrayList<>(window);
        for (TouristSpot spot : master) {
            if (taken.size() >= window) {
                break;
            }
            if (globalUsedIds.contains(spot.getContentId()) || !WeekdayLabels.isOpenOn(spot, weekday)) {
                continue;
            }
            taken.add(spot);
        }
        globalUsedIds.addAll(taken.stream().map(TouristSpot::getContentId).toList());
        return taken;
    }

    /** 선택권을 주기 위해 실제 필요 수보다 여유분만큼 더 뽑아 후보로 제시한다(미선택분도 다른 날짜에서는 잠긴 상태). */
    private int windowSize(int needed) {
        return needed <= 0 ? 0 : needed + CANDIDATE_WINDOW_EXTRA;
    }

    private List<TouristSpot> fetchByCategory(List<Integer> categories, String sigunguCode, float[] queryEmbedding, int limit) {

        if (queryEmbedding == null) {
            return touristSpotRepository.findByCategoryInAndSigunguCodeOrderByAvgRatingDesc(categories, sigunguCode, PageRequest.of(0, limit));
        }
        List<Long> nearestIds = spotEmbeddingRepository.findNearestContentIdsInCategories(queryEmbedding, categories, List.of(), sigunguCode, limit);

        return resolveSpots(nearestIds);
    }

    private List<TouristSpot> resolveSpots(List<Long> contentIds) {
        Map<Long, TouristSpot> spotsById = touristSpotRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));
        return contentIds.stream().map(spotsById::get).filter(Objects::nonNull).toList();
    }

    //리스트 -> 딕셔너리 형태로 변환 : 검증단계에서 한번에 서치할수 있도록 Map get함수 : O(1), 리스트 :O(N)
    private Map<Long, TouristSpot> toMap(List<TouristSpot> spots) {
        return spots.stream().collect(Collectors.toMap(TouristSpot::getContentId, Function.identity(), (left, right) -> left));
    }

    private int poolLimit(int perDaySlots, int tripDays) {
        return Math.min(CANDIDATE_LIMIT_CAP, Math.max(perDaySlots, perDaySlots * tripDays * CANDIDATE_BUFFER));
    }
}
