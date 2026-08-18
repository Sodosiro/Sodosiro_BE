package com.sodosiro.domain.course.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodosiro.domain.course.constants.TravelStyle;
import com.sodosiro.domain.course.controller.dto.CourseRecommendRequest;
import com.sodosiro.domain.course.controller.dto.CourseRecommendResponse;
import com.sodosiro.domain.course.service.dto.CandidatePoolResult;
import com.sodosiro.domain.course.service.dto.CandidateSpot;
import com.sodosiro.domain.course.service.dto.DayCandidatePool;
import com.sodosiro.domain.course.service.dto.LlmCourseDay;
import com.sodosiro.domain.course.service.dto.LlmCourseResponse;
import com.sodosiro.domain.course.service.dto.LlmRequestPayload;
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
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 일자별로 격리된 후보 풀(Day-specific Pool) 구성 → LLM에게 각 날짜 자신의 풀 안에서만 코스 설계를 위임 → 응답 검증까지 담당한다.
 * 여행 전체 기간을 관통하는 전역 중복 방지 장바구니({@code globalUsedIds})를 두고, 날짜를 순회하며 마스터 풀에서
 * 그날 필요한 개수보다 조금 넉넉하게(여유분 {@value #CANDIDATE_WINDOW_EXTRA}) 뽑아 즉시 잠그기 때문에,
 * 이후 날짜의 풀에는 애초에 등장하지 않는다. 휴무일도 그날의 풀을 만드는 시점에 걸러내므로 LLM은 "중복 금지/휴무 회피"를
 * 스스로 추론할 필요 없이 그 날짜의 풀 안에서 순서만 정하면 된다.
 * mustVisit이 배정될 날짜는 호출자(CourseRecommendationService)가 사전에 확정해 넘겨준다.
 * 검증 실패 시 최대 {@value #MAX_ATTEMPTS}회 시도하고, 그래도 실패하면 빈 Optional을 반환해
 * 호출자가 규칙기반 알고리즘으로 폴백하도록 신호한다.
 */
@Slf4j
@Service
public class CourseAiPlanner {

    private static final int DAILY_SLOT_COUNT = 5;
    private static final int RESTAURANT_SLOTS_PER_DAY = 2;
    private static final int CANDIDATE_BUFFER = 3;
    private static final int CANDIDATE_LIMIT_CAP = 200;
    private static final int CANDIDATE_WINDOW_EXTRA = 2;
    private static final int MAX_ATTEMPTS = 2;

    private static final String SYSTEM_PROMPT = """
            당신은 여행 코스 설계 전문가입니다. 입력의 days 배열은 날짜별로 이미 격리된 후보 풀입니다. \
            각 날짜의 restaurants/styleCandidates/randomPool은 그 날짜의 요일 영업 여부와 여행 전체 기간 중복을 \
            이미 걸러낸 상태이므로, 반드시 그 날짜 자신의 풀 안에서만 골라야 합니다.

            규칙:
            - 하루는 정확히 5곳으로 구성합니다: 그 날짜의 restaurants 중 2곳(점심·저녁 1,4번째 순서에 오도록 배치) \
            + styleCandidates의 각 카테고리에서 제공된 경우 1곳씩 + mustVisit이 주어진 날짜라면 그 스팟을 반드시 포함 \
            + 남은 자리는 그 날짜의 randomPool에서 채웁니다.
            - contentIds 배열의 순서는 실제 방문 순서입니다. 동선을 고려해 지리적으로 합리적인 순서로 배치하세요.
            - 각 날짜의 contentIds는 반드시 그 날짜 자신의 restaurants/styleCandidates/randomPool/mustVisit 안의 id만 \
            사용하세요. 다른 날짜의 풀이나 존재하지 않는 id를 만들어내지 마세요.
            - 같은 날짜 안에서 같은 id를 두 번 사용하지 마세요.
            - 아래 JSON 스키마로만 응답하세요. 설명, 마크다운, 코드펜스 없이 순수 JSON 객체 하나만 출력하세요.

            {"days":[{"day":1,"contentIds":[111,222,333,444,555]}]}
            """;

    private final TouristSpotRepository touristSpotRepository;
    private final SpotEmbeddingRepository spotEmbeddingRepository;
    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;

    public CourseAiPlanner(TouristSpotRepository touristSpotRepository, SpotEmbeddingRepository spotEmbeddingRepository, ObjectMapper objectMapper, ChatModel chatModel) {
        this.touristSpotRepository = touristSpotRepository;
        this.spotEmbeddingRepository = spotEmbeddingRepository;
        this.objectMapper = objectMapper;
        this.chatClient = ChatClient.create(chatModel);
    }

    public Optional<List<CourseRecommendResponse.DayCourse>> tryGenerate(CourseRecommendRequest request, List<LocalDate> dates, TouristSpot mustVisitSpot, int mustVisitDayIndex, float[] queryEmbedding) {

        try {
            CandidatePoolResult pool = buildCandidatePool(request, dates, queryEmbedding, mustVisitSpot, mustVisitDayIndex);
            String userPrompt = objectMapper.writeValueAsString(new LlmRequestPayload(dates.size(), pool.dayPools()));

            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                String responseText = chatClient.prompt().system(SYSTEM_PROMPT).user(userPrompt).call().content();
                Optional<List<CourseRecommendResponse.DayCourse>> validated =
                        parseAndValidate(responseText, dates, pool, mustVisitSpot, mustVisitDayIndex);
                if (validated.isPresent()) {
                    return validated;
                }
                log.warn("AI 코스 검증 실패(시도 {}/{})", attempt, MAX_ATTEMPTS);
            }
            return Optional.empty();
        } catch (Exception exception) {
            log.warn("AI 코스 설계 실패, 규칙기반으로 폴백", exception);
            return Optional.empty();
        }
    }

    // ---------- 1단계: Day-specific Pool 구성 ----------

    private CandidatePoolResult buildCandidatePool(CourseRecommendRequest request, List<LocalDate> dates, float[] queryEmbedding, TouristSpot mustVisitSpot, int mustVisitDayIndex) {

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
            for (Integer code : styleCodes) {
                int needed = (mustVisitHere && code.equals(mustVisitCategory)) ? 0 : 1;
                List<TouristSpot> picked = takeForDay(styleMaster.get(code), globalUsedIds, weekday, windowSize(needed));
                byId.putAll(toMap(picked));
                styleForDay.put(code, picked.stream().map(CandidateSpot::from).toList());
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
        }

        return new CandidatePoolResult(dayPools, byId);
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

    /** LLM에게 선택권을 주기 위해 실제 필요 수보다 여유분만큼 더 뽑아 후보로 제시한다(미선택분도 다른 날짜에서는 잠긴 상태). */
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

    // ---------- 2단계: Validation ----------

    private Optional<List<CourseRecommendResponse.DayCourse>> parseAndValidate(
            String responseText, List<LocalDate> dates, CandidatePoolResult pool,
            TouristSpot mustVisitSpot, int mustVisitDayIndex) {
        LlmCourseResponse response;
        try {
            response = objectMapper.readValue(stripCodeFence(responseText), LlmCourseResponse.class);
        } catch (Exception exception) {
            log.warn("AI 코스 응답 파싱 실패: {}", responseText, exception);
            return Optional.empty();
        }
        if (response == null || response.days() == null || response.days().size() != dates.size()) {
            return Optional.empty();
        }

        Long mustVisitId = mustVisitSpot == null ? null : mustVisitSpot.getContentId();
        Map<Integer, LlmCourseDay> byDay = new TreeMap<>();

        for (LlmCourseDay day : response.days()) {
            if (day.day() < 1 || day.day() > dates.size() || byDay.containsKey(day.day())) {
                return Optional.empty();
            }
            if (day.contentIds() == null || day.contentIds().size() != DAILY_SLOT_COUNT
                    || new HashSet<>(day.contentIds()).size() != DAILY_SLOT_COUNT) {
                return Optional.empty();
            }

            DayCandidatePool dayPool = pool.dayPools().get(day.day() - 1);
            Set<Long> validIds = validIdsFor(dayPool);

            int restaurantCount = 0;
            boolean mustVisitIncluded = false;
            for (Long contentId : day.contentIds()) {
                //그 날짜 자신의 풀 안의 id만 허용: 다른 날짜의 풀은 애초에 이 Set에 없으므로 전역 중복도 여기서 함께 걸러진다.
                if (!validIds.contains(contentId)) {
                    return Optional.empty();
                }
                TouristSpot spot = pool.byId().get(contentId);
                if (Objects.equals(spot.getCategory(), TravelStyle.RESTAURANT.categoryCode())) {
                    restaurantCount++;
                }
                if (mustVisitId != null && mustVisitId.equals(contentId)) {
                    mustVisitIncluded = true;
                }
            }
            if (restaurantCount != RESTAURANT_SLOTS_PER_DAY) {
                return Optional.empty();
            }
            if (mustVisitId != null && day.day() - 1 == mustVisitDayIndex && !mustVisitIncluded) {
                return Optional.empty();
            }
            byDay.put(day.day(), day);
        }

        List<CourseRecommendResponse.DayCourse> result = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            LlmCourseDay day = byDay.get(i + 1);
            List<CourseRecommendResponse.RecommendedSpot> spots = day.contentIds().stream()
                    .map(contentId -> toRecommendedSpot(pool.byId().get(contentId), contentId.equals(mustVisitId)))
                    .toList();
            result.add(new CourseRecommendResponse.DayCourse(i + 1, dates.get(i), spots));
        }
        return Optional.of(result);
    }

    private Set<Long> validIdsFor(DayCandidatePool dayPool) {
        Set<Long> ids = new HashSet<>();
        dayPool.restaurants().forEach(candidate -> ids.add(candidate.id()));
        dayPool.styleCandidates().values().forEach(list -> list.forEach(candidate -> ids.add(candidate.id())));
        dayPool.randomPool().forEach(candidate -> ids.add(candidate.id()));
        if (dayPool.mustVisit() != null) {
            ids.add(dayPool.mustVisit().id());
        }
        return ids;
    }

    private String stripCodeFence(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```[a-zA-Z]*\\n?", "");
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence >= 0) {
                trimmed = trimmed.substring(0, lastFence);
            }
        }
        return trimmed.trim();
    }

    private CourseRecommendResponse.RecommendedSpot toRecommendedSpot(TouristSpot spot, boolean mustVisit) {
        return new CourseRecommendResponse.RecommendedSpot(
                spot.getContentId(), spot.getTitle(), spot.getFirstImage(),
                spot.getMapX(), spot.getMapY(), spot.getCategory(), mustVisit);
    }
}
