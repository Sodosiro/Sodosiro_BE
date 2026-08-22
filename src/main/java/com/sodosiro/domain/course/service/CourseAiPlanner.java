package com.sodosiro.domain.course.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodosiro.domain.course.constants.TravelStyle;
import com.sodosiro.domain.course.controller.dto.CourseRecommendRequest;
import com.sodosiro.domain.course.controller.dto.CourseRecommendResponse;
import com.sodosiro.domain.course.service.dto.CandidatePoolResult;
import com.sodosiro.domain.course.service.dto.DayCandidatePool;
import com.sodosiro.domain.course.service.dto.LlmCourseDay;
import com.sodosiro.domain.course.service.dto.LlmCourseResponse;
import com.sodosiro.domain.course.service.dto.LlmRequestPayload;
import com.sodosiro.domain.travel.entity.TouristSpot;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Service;

/**
 * {@link CourseCandidatePoolBuilder}가 만든 일자별로 격리된 후보 풀을 LLM에게 넘겨 각 날짜 자신의 풀 안에서만
 * 코스 설계를 위임하고, 응답 검증까지 담당한다. 검증 실패 시 최대 {@value #MAX_ATTEMPTS}회 시도하고, 그래도
 * 실패하면 빈 Optional을 반환해 호출자가 {@link CourseRuleBasedPlanner}로 폴백하도록 신호한다.
 */
@Slf4j
@Service
public class CourseAiPlanner {

    private static final int DAILY_SLOT_COUNT = CourseCandidatePoolBuilder.DAILY_SLOT_COUNT;
    private static final int RESTAURANT_SLOTS_PER_DAY = CourseCandidatePoolBuilder.RESTAURANT_SLOTS_PER_DAY;
    private static final int MAX_ATTEMPTS = 2;

    private static final String SYSTEM_PROMPT = """
            당신은 여행 코스 설계 전문가입니다. 입력의 days 배열은 날짜별로 이미 격리된 후보 풀입니다. \
            각 날짜의 restaurants/styleCandidates/randomPool은 그 날짜의 요일 영업 여부와 여행 전체 기간 중복을 \
            이미 걸러낸 상태이므로, 반드시 그 날짜 자신의 풀 안에서만 골라야 합니다.

            규칙:
            - 하루는 정확히 5곳으로 구성합니다: 그 날짜의 restaurants 중 2곳(점심·저녁) \
            + styleCandidates의 각 카테고리에서 제공된 경우 1곳씩 + mustVisit이 주어진 날짜라면 그 스팟을 반드시 포함 \
            + 남은 자리는 그 날짜의 randomPool에서 채웁니다.
            - contentIds 배열의 순서는 실제 방문 순서입니다. 동선을 고려해 지리적으로 합리적인 순서로 배치하세요. \
            식당 2곳의 정확한 위치(몇 번째 순서인지)는 서버가 점심·저녁 자리에 맞게 재배치하므로, \
            식당을 제외한 나머지 스팟들 사이의 동선만 신경 쓰면 됩니다.
            - 각 날짜의 contentIds는 반드시 그 날짜 자신의 restaurants/styleCandidates/randomPool/mustVisit 안의 id만 \
            사용하세요. 다른 날짜의 풀이나 존재하지 않는 id를 만들어내지 마세요.
            - 같은 날짜 안에서 같은 id를 두 번 사용하지 마세요.
            - 아래 JSON 스키마로만 응답하세요. 설명, 마크다운, 코드펜스 없이 순수 JSON 객체 하나만 출력하세요.

            {"days":[{"day":1,"contentIds":[111,222,333,444,555]}]}
            """;

    private final CourseCandidatePoolBuilder candidatePoolBuilder;
    private final ObjectMapper objectMapper;
    private final ChatClient chatClient;

    public CourseAiPlanner(CourseCandidatePoolBuilder candidatePoolBuilder, ObjectMapper objectMapper, ChatModel chatModel) {
        this.candidatePoolBuilder = candidatePoolBuilder;
        this.objectMapper = objectMapper;
        this.chatClient = ChatClient.create(chatModel);
    }

    public Optional<List<CourseRecommendResponse.DayCourse>> tryGenerate(CourseRecommendRequest request, List<LocalDate> dates, TouristSpot mustVisitSpot, int mustVisitDayIndex, float[] queryEmbedding) {

        try {
            CandidatePoolResult pool = candidatePoolBuilder.build(request, dates, queryEmbedding, mustVisitSpot, mustVisitDayIndex);
            String userPrompt = objectMapper.writeValueAsString(new LlmRequestPayload(dates.size(), pool.dayPools()));

            for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
                String responseText = chatClient.prompt().system(SYSTEM_PROMPT).user(userPrompt).call().content();
                String[] failReason = new String[1];
                Optional<List<CourseRecommendResponse.DayCourse>> validated =
                        parseAndValidate(responseText, dates, pool, mustVisitSpot, mustVisitDayIndex, failReason);
                if (validated.isPresent()) {
                    return validated;
                }
                log.warn("AI 코스 검증 실패(시도 {}/{}): {}", attempt, MAX_ATTEMPTS, failReason[0]);
            }
            return Optional.empty();
        } catch (Exception exception) {
            log.warn("AI 코스 설계 실패, 규칙기반으로 폴백", exception);
            return Optional.empty();
        }
    }

    // ---------- Validation ----------

    private Optional<List<CourseRecommendResponse.DayCourse>> parseAndValidate(
            String responseText, List<LocalDate> dates, CandidatePoolResult pool,
            TouristSpot mustVisitSpot, int mustVisitDayIndex, String[] failReason) {
        LlmCourseResponse response;
        try {
            response = objectMapper.readValue(stripCodeFence(responseText), LlmCourseResponse.class);
        } catch (Exception exception) {
            log.warn("AI 코스 응답 파싱 실패: {}", responseText, exception);
            failReason[0] = "JSON 파싱 실패";
            return Optional.empty();
        }
        if (response == null || response.days() == null || response.days().size() != dates.size()) {
            failReason[0] = "days 개수 불일치(응답=%s, 기대=%d)".formatted(
                    response == null || response.days() == null ? "null" : response.days().size(), dates.size());
            return Optional.empty();
        }

        Long mustVisitId = mustVisitSpot == null ? null : mustVisitSpot.getContentId();
        Map<Integer, LlmCourseDay> byDay = new TreeMap<>();

        for (LlmCourseDay day : response.days()) {
            if (day.day() < 1 || day.day() > dates.size() || byDay.containsKey(day.day())) {
                failReason[0] = "day 필드 이상(day=%d)".formatted(day.day());
                return Optional.empty();
            }
            if (day.contentIds() == null || day.contentIds().size() != DAILY_SLOT_COUNT
                    || new HashSet<>(day.contentIds()).size() != DAILY_SLOT_COUNT) {
                failReason[0] = "day %d contentIds 개수/중복 오류(%s)".formatted(day.day(), day.contentIds());
                return Optional.empty();
            }

            DayCandidatePool dayPool = pool.dayPools().get(day.day() - 1);
            Set<Long> validIds = validIdsFor(dayPool);

            int restaurantCount = 0;
            boolean mustVisitIncluded = false;
            for (Long contentId : day.contentIds()) {
                //그 날짜 자신의 풀 안의 id만 허용: 다른 날짜의 풀은 애초에 이 Set에 없으므로 전역 중복도 여기서 함께 걸러진다.
                if (!validIds.contains(contentId)) {
                    failReason[0] = "day %d 유효하지 않은 contentId=%d(풀=%s)".formatted(day.day(), contentId, validIds);
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
                failReason[0] = "day %d 식당 개수 불일치(응답=%d, 기대=%d)".formatted(day.day(), restaurantCount, RESTAURANT_SLOTS_PER_DAY);
                return Optional.empty();
            }
            if (mustVisitId != null && day.day() - 1 == mustVisitDayIndex && !mustVisitIncluded) {
                failReason[0] = "day %d mustVisit(%d) 누락".formatted(day.day(), mustVisitId);
                return Optional.empty();
            }
            byDay.put(day.day(), day);
        }

        List<CourseRecommendResponse.DayCourse> result = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            LlmCourseDay day = byDay.get(i + 1);
            List<Long> orderedContentIds = MealSlotOrdering.placeRestaurantsAtMealSlots(day.contentIds(), pool.byId());
            List<CourseRecommendResponse.RecommendedSpot> spots = orderedContentIds.stream()
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
