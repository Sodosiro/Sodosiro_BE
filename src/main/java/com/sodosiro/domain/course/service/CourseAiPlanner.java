package com.sodosiro.domain.course.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodosiro.domain.course.constants.TravelStyle;
import com.sodosiro.domain.course.controller.dto.CourseRecommendRequest;
import com.sodosiro.domain.course.controller.dto.CourseRecommendResponse;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.SpotEmbeddingRepository;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import java.math.BigDecimal;
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
 * 후보 풀(retrieval) 구성 → LLM에게 일자별 코스 설계 위임 → 응답 검증까지 담당한다.
 * 검증 실패 시 최대 {@value #MAX_ATTEMPTS}회 시도하고, 그래도 실패하면 빈 Optional을 반환해
 * 호출자(CourseRecommendationService)가 규칙기반 알고리즘으로 폴백하도록 신호한다.
 */
@Slf4j
@Service
public class CourseAiPlanner {

    private static final int DAILY_SLOT_COUNT = 5;
    private static final int RESTAURANT_SLOTS_PER_DAY = 2;
    private static final int CANDIDATE_BUFFER = 3;
    private static final int CANDIDATE_LIMIT_CAP = 200;
    private static final int MAX_ATTEMPTS = 2;

    private static final String SYSTEM_PROMPT = """
            당신은 여행 코스 설계 전문가입니다. 아래 후보 관광지 목록 안에서만 골라 여행 전체 일정을 설계하세요.

            규칙:
            - 하루는 정확히 5곳으로 구성합니다: restaurants 후보 중 2곳(점심·저녁 시간대에 오도록 순서 배치) \
            + styleCandidates의 각 카테고리에서 1곳씩(제공된 경우) + 남은 자리는 randomPool에서 채웁니다.
            - contentIds 배열의 순서는 실제 방문 순서입니다. 동선을 고려해 지리적으로 합리적인 순서로 배치하세요.
            - mustVisit이 주어지면, 전체 여행 기간 중 정확히 하루의 5개 슬롯 중 하나로 배치하세요(추가 슬롯이 아닙니다). \
            dayInfos의 weekday와 스팟의 restdate가 겹치지 않는 날짜를 고르세요.
            - 모든 스팟은 배정된 날짜의 요일이 그 스팟의 restdate(휴무 요일 문자열)와 겹치면 안 됩니다.
            - 같은 id를 전체 여행 기간 동안 두 번 이상 사용하지 마세요.
            - 반드시 제공된 후보의 id만 사용하세요. 존재하지 않는 id를 만들어내지 마세요.
            - 아래 JSON 스키마로만 응답하세요. 설명, 마크다운, 코드펜스 없이 순수 JSON 객체 하나만 출력하세요.

            {"days":[{"day":1,"contentIds":[111,222,333,444,555]}]}
            """;

    private final TouristSpotRepository touristSpotRepository;
    private final SpotEmbeddingRepository spotEmbeddingRepository;
    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;

    public CourseAiPlanner(
            TouristSpotRepository touristSpotRepository, SpotEmbeddingRepository spotEmbeddingRepository,
            ObjectMapper objectMapper, ChatModel chatModel) {
        this.touristSpotRepository = touristSpotRepository;
        this.spotEmbeddingRepository = spotEmbeddingRepository;
        this.objectMapper = objectMapper;
        this.chatClient = ChatClient.create(chatModel);
    }

    public Optional<List<CourseRecommendResponse.DayCourse>> tryGenerate(
            CourseRecommendRequest request, List<LocalDate> dates, TouristSpot mustVisitSpot, float[] queryEmbedding) {
        try {
            CandidatePool pool = buildCandidatePool(request, dates.size(), queryEmbedding, mustVisitSpot);
            String userPrompt = objectMapper.writeValueAsString(buildPayload(dates, pool));

            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                String responseText = chatClient.prompt().system(SYSTEM_PROMPT).user(userPrompt).call().content();
                Optional<List<CourseRecommendResponse.DayCourse>> validated = parseAndValidate(responseText, dates, pool);
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

    // ---------- 1단계: Retrieval ----------

    private CandidatePool buildCandidatePool(
            CourseRecommendRequest request, int tripDays, float[] queryEmbedding, TouristSpot mustVisitSpot) {
        Map<Long, TouristSpot> byId = new LinkedHashMap<>();

        List<Integer> restaurantCategory = List.of(TravelStyle.RESTAURANT.categoryCode());
        List<TouristSpot> restaurantSpots = fetchByCategory(
                restaurantCategory, request.sigunguCode(), queryEmbedding, poolLimit(RESTAURANT_SLOTS_PER_DAY, tripDays));
        byId.putAll(toMap(restaurantSpots));

        List<Integer> styleCodes = request.travelStylesOrEmpty().stream()
                .filter(style -> style != TravelStyle.RESTAURANT)
                .map(TravelStyle::categoryCode)
                .toList();
        Map<Integer, List<TouristSpot>> styleSpots = new LinkedHashMap<>();
        for (Integer code : styleCodes) {
            List<TouristSpot> spots = fetchByCategory(
                    List.of(code), request.sigunguCode(), queryEmbedding, poolLimit(1, tripDays));
            styleSpots.put(code, spots);
            byId.putAll(toMap(spots));
        }

        List<Integer> nonRestaurantCategories = Arrays.stream(TravelStyle.values())
                .filter(style -> style != TravelStyle.RESTAURANT)
                .map(TravelStyle::categoryCode)
                .toList();
        int remainingSlots = Math.max(1, DAILY_SLOT_COUNT - RESTAURANT_SLOTS_PER_DAY - styleCodes.size());
        List<TouristSpot> randomSpots = fetchByCategory(
                nonRestaurantCategories, request.sigunguCode(), queryEmbedding, poolLimit(remainingSlots, tripDays));
        byId.putAll(toMap(randomSpots));

        if (mustVisitSpot != null) {
            byId.put(mustVisitSpot.getContentId(), mustVisitSpot);
        }

        return new CandidatePool(
                restaurantSpots.stream().map(CandidateSpot::from).toList(),
                styleSpots.entrySet().stream().collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().stream().map(CandidateSpot::from).toList(),
                        (left, right) -> left, LinkedHashMap::new)),
                randomSpots.stream().map(CandidateSpot::from).toList(),
                mustVisitSpot == null ? null : CandidateSpot.from(mustVisitSpot),
                byId);
    }

    private List<TouristSpot> fetchByCategory(
            List<Integer> categories, String sigunguCode, float[] queryEmbedding, int limit) {
        if (queryEmbedding == null) {
            return touristSpotRepository.findByCategoryInAndSigunguCodeOrderByAvgRatingDesc(
                    categories, sigunguCode, PageRequest.of(0, limit));
        }
        List<Long> nearestIds = spotEmbeddingRepository.findNearestContentIdsInCategories(
                queryEmbedding, categories, List.of(), sigunguCode, limit);
        return resolveSpots(nearestIds);
    }

    private List<TouristSpot> resolveSpots(List<Long> contentIds) {
        Map<Long, TouristSpot> spotsById = touristSpotRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));
        return contentIds.stream().map(spotsById::get).filter(Objects::nonNull).toList();
    }

    private Map<Long, TouristSpot> toMap(List<TouristSpot> spots) {
        return spots.stream().collect(Collectors.toMap(TouristSpot::getContentId, Function.identity(), (left, right) -> left));
    }

    private int poolLimit(int perDaySlots, int tripDays) {
        return Math.min(CANDIDATE_LIMIT_CAP, Math.max(perDaySlots, perDaySlots * tripDays * CANDIDATE_BUFFER));
    }

    // ---------- 2단계: LLM Design ----------

    private LlmRequestPayload buildPayload(List<LocalDate> dates, CandidatePool pool) {
        List<DayInfo> dayInfos = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = dates.get(i);
            dayInfos.add(new DayInfo(i + 1, date.toString(), WeekdayLabels.labelOf(date)));
        }
        Long mustVisitId = pool.mustVisit() == null ? null : pool.mustVisit().id();
        return new LlmRequestPayload(
                dates.size(), dayInfos, mustVisitId, pool.mustVisit(),
                pool.restaurants(), pool.styleCandidates(), pool.randomPool());
    }

    // ---------- 3단계: Validation ----------

    private Optional<List<CourseRecommendResponse.DayCourse>> parseAndValidate(
            String responseText, List<LocalDate> dates, CandidatePool pool) {
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

        Long mustVisitId = pool.mustVisit() == null ? null : pool.mustVisit().id();
        Map<Integer, LlmCourseDay> byDay = new TreeMap<>();
        Set<Long> seenContentIds = new HashSet<>();
        int mustVisitCount = 0;

        for (LlmCourseDay day : response.days()) {
            if (day.day() < 1 || day.day() > dates.size() || byDay.containsKey(day.day())) {
                return Optional.empty();
            }
            if (day.contentIds() == null || day.contentIds().size() != DAILY_SLOT_COUNT) {
                return Optional.empty();
            }

            LocalDate date = dates.get(day.day() - 1);
            String weekdayLabel = WeekdayLabels.labelOf(date);
            int restaurantCount = 0;
            for (Long contentId : day.contentIds()) {
                if (!seenContentIds.add(contentId)) {
                    return Optional.empty();
                }
                TouristSpot spot = pool.byId().get(contentId);
                if (spot == null || !WeekdayLabels.isOpenOn(spot, weekdayLabel)) {
                    return Optional.empty();
                }
                if (Objects.equals(spot.getCategory(), TravelStyle.RESTAURANT.categoryCode())) {
                    restaurantCount++;
                }
                if (mustVisitId != null && mustVisitId.equals(contentId)) {
                    mustVisitCount++;
                }
            }
            if (restaurantCount != RESTAURANT_SLOTS_PER_DAY) {
                return Optional.empty();
            }
            byDay.put(day.day(), day);
        }
        if (mustVisitId != null && mustVisitCount != 1) {
            return Optional.empty();
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

    // ---------- 내부 DTO ----------

    private record CandidateSpot(
            Long id, String title, Integer category, BigDecimal mapX, BigDecimal mapY,
            BigDecimal avgRating, String restdate) {
        static CandidateSpot from(TouristSpot spot) {
            return new CandidateSpot(
                    spot.getContentId(), spot.getTitle(), spot.getCategory(),
                    spot.getMapX(), spot.getMapY(), spot.getAvgRating(), spot.getRestdate());
        }
    }

    private record DayInfo(int day, String date, String weekday) {
    }

    private record LlmRequestPayload(
            int tripDays, List<DayInfo> dayInfos, Long mustVisitId, CandidateSpot mustVisit,
            List<CandidateSpot> restaurants, Map<Integer, List<CandidateSpot>> styleCandidates,
            List<CandidateSpot> randomPool) {
    }

    private record CandidatePool(
            List<CandidateSpot> restaurants, Map<Integer, List<CandidateSpot>> styleCandidates,
            List<CandidateSpot> randomPool, CandidateSpot mustVisit, Map<Long, TouristSpot> byId) {
    }

    private record LlmCourseDay(int day, List<Long> contentIds) {
    }

    private record LlmCourseResponse(List<LlmCourseDay> days) {
    }
}
