package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.controller.dto.CourseRecommendRequest;
import com.sodosiro.domain.course.controller.dto.CourseRecommendResponse;
import com.sodosiro.domain.course.controller.dto.TravelStyle;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.SpotEmbeddingRepository;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * 여행스타일 카테고리 필터 + AI 한마디 임베딩 유사도를 섞은 후보군에서 하루 일정을 채운다.
 * 하루 5개 슬롯 = 필수 슬롯 3개(식당 1, 카페 1, 관광/자연/액티비티/쇼핑 1) + 자율 슬롯 2개(후보 풀 상위에서 자유롭게)로 구성하고,
 * 요일 휴무는 전 슬롯에서 제외하며, 같은 날은 동선(위경도) 근접순으로 정렬한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseRecommendationService {

    private static final int FREE_SLOTS_PER_DAY = 2;
    private static final int EMBEDDING_POOL_LIMIT = 60;

    private static final Map<DayOfWeek, String> WEEKDAY_LABELS = Map.of(
            DayOfWeek.MONDAY, "월요일",
            DayOfWeek.TUESDAY, "화요일",
            DayOfWeek.WEDNESDAY, "수요일",
            DayOfWeek.THURSDAY, "목요일",
            DayOfWeek.FRIDAY, "금요일",
            DayOfWeek.SATURDAY, "토요일",
            DayOfWeek.SUNDAY, "일요일"
    );

    private final TouristSpotRepository touristSpotRepository;
    private final SpotEmbeddingRepository spotEmbeddingRepository;
    private final EmbeddingModel embeddingModel;

    public CourseRecommendResponse recommend(CourseRecommendRequest request) {

        validateDateRange(request.startDate(), request.endDate());

        List<LocalDate> dates = buildDateRange(request.startDate(), request.endDate());

        TouristSpot mustVisitSpot = request.mustVisitContentId() == null
                ? null
                : findTouristSpot(request.mustVisitContentId());

        List<Integer> categoryCodes = request.travelStylesOrEmpty().stream()
                .map(TravelStyle::categoryCode)
                .toList();
        List<TouristSpot> candidatePool = buildCandidatePool(categoryCodes, request.aiMessage());

        Set<Long> usedContentIds = new HashSet<>();
        int mustVisitDayIndex = -1;
        if (mustVisitSpot != null) {
            usedContentIds.add(mustVisitSpot.getContentId());
            mustVisitDayIndex = resolveMustVisitDayIndex(mustVisitSpot, dates);
        }

        List<CourseRecommendResponse.DayCourse> days = new ArrayList<>();
        for (int i = 0; i < dates.size(); i++) {
            LocalDate date = dates.get(i);
            String weekdayLabel = WEEKDAY_LABELS.get(date.getDayOfWeek());
            boolean assignMustVisit = i == mustVisitDayIndex;
            RequiredSlotGroup satisfiedGroup = assignMustVisit
                    ? RequiredSlotGroup.groupOf(mustVisitSpot.getCategory()).orElse(null)
                    : null;

            List<TouristSpot> picked = pickForDay(candidatePool, usedContentIds, weekdayLabel, satisfiedGroup);
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
        return new CourseRecommendResponse(days);
    }

    private void validateDateRange(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료일은 시작일보다 빠를 수 없습니다.");
        }
    }

    private List<LocalDate> buildDateRange(LocalDate startDate, LocalDate endDate) {
        long dayCount = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        return LongStream.range(0, dayCount).mapToObj(startDate::plusDays).toList();
    }

    private TouristSpot findTouristSpot(Long contentId) {
        return touristSpotRepository.findById(contentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행지를 찾을 수 없습니다."));
    }

    /** mustVisit이 휴무가 아닌 첫 날짜에 배치한다. 여행 기간 내내 휴무면 첫째 날에 배치한다. */
    private int resolveMustVisitDayIndex(TouristSpot mustVisitSpot, List<LocalDate> dates) {
        for (int i = 0; i < dates.size(); i++) {
            String weekdayLabel = WEEKDAY_LABELS.get(dates.get(i).getDayOfWeek());
            if (!isClosedOn(mustVisitSpot, weekdayLabel)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * 임베딩 유사도 후보(카테고리 선택 시 해당 카테고리를 가중치로 우선 배치) + 카테고리 후보 + 인기순 폴백 후보를
     * 이 순서로 합쳐(중복 제거) 하나의 풀로 만든다. 카테고리는 후보를 걸러내는 필터가 아니라
     * AI 한마디 유사도 검색 결과 안에서 우선순위를 높이는 가중치로 쓴다.
     */
    private List<TouristSpot> buildCandidatePool(List<Integer> categoryCodes, String aiMessage) {
        List<TouristSpot> embeddingCandidates = (aiMessage == null || aiMessage.isBlank())
                ? List.of()
                : findEmbeddingCandidates(aiMessage, categoryCodes);

        List<TouristSpot> categoryCandidates = categoryCodes.isEmpty()
                ? List.of()
                : touristSpotRepository.findTop200ByCategoryInOrderByAvgRatingDesc(categoryCodes);

        List<TouristSpot> fallbackCandidates = touristSpotRepository.findTop200ByOrderByAvgRatingDesc();

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

    private List<TouristSpot> findEmbeddingCandidates(String aiMessage, List<Integer> categoryCodes) {
        float[] queryEmbedding = embeddingModel.embed(aiMessage);
        List<Long> nearestIds = spotEmbeddingRepository
                .findNearestContentIds(queryEmbedding, categoryCodes, EMBEDDING_POOL_LIMIT);
        Map<Long, TouristSpot> spotsById = touristSpotRepository.findAllById(nearestIds).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));
        return nearestIds.stream().map(spotsById::get).filter(Objects::nonNull).toList();
    }

    /**
     * 필수 슬롯(satisfiedGroup으로 이미 채워진 그룹은 제외) 각 1개 + 자율 슬롯 {@value #FREE_SLOTS_PER_DAY}개를 채운다.
     * 자율 슬롯은 후보 풀 상위 순서를 그대로 따르므로 여행스타일/AI 한마디 가중치가 자연스럽게 반영된다.
     */
    private List<TouristSpot> pickForDay(
            List<TouristSpot> pool, Set<Long> usedContentIds, String weekdayLabel, RequiredSlotGroup satisfiedGroup) {
        Set<Long> excluded = new HashSet<>(usedContentIds);
        List<TouristSpot> picked = new ArrayList<>();

        for (RequiredSlotGroup group : RequiredSlotGroup.values()) {
            if (group == satisfiedGroup) {
                continue;
            }
            TouristSpot spot = pickForGroup(pool, excluded, weekdayLabel, group)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, group.displayName + " 카테고리에 해당하는 관광지가 부족합니다."));
            picked.add(spot);
            excluded.add(spot.getContentId());
        }

        picked.addAll(pickFreeSlots(pool, excluded, weekdayLabel, FREE_SLOTS_PER_DAY));
        return picked;
    }

    /** 후보 풀에서 먼저 찾고, 풀에 없으면 해당 그룹 카테고리로 직접 조회해 채운다. */
    private Optional<TouristSpot> pickForGroup(
            List<TouristSpot> pool, Set<Long> excluded, String weekdayLabel, RequiredSlotGroup group) {
        for (TouristSpot spot : pool) {
            if (group.matches(spot.getCategory())
                    && !excluded.contains(spot.getContentId())
                    && !isClosedOn(spot, weekdayLabel)) {
                return Optional.of(spot);
            }
        }
        List<TouristSpot> broaderCandidates = touristSpotRepository
                .findTop200ByCategoryInOrderByAvgRatingDesc(group.categoryCodes);
        for (TouristSpot spot : broaderCandidates) {
            if (!excluded.contains(spot.getContentId()) && !isClosedOn(spot, weekdayLabel)) {
                return Optional.of(spot);
            }
        }
        return Optional.empty();
    }

    private List<TouristSpot> pickFreeSlots(
            List<TouristSpot> pool, Set<Long> excluded, String weekdayLabel, int quota) {
        List<TouristSpot> picked = new ArrayList<>(quota);
        for (TouristSpot spot : pool) {
            if (picked.size() >= quota) {
                break;
            }
            if (excluded.contains(spot.getContentId()) || isClosedOn(spot, weekdayLabel)) {
                continue;
            }
            picked.add(spot);
        }
        if (picked.size() < quota) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "조건에 맞는 관광지가 부족합니다.");
        }
        return picked;
    }

    /** 필수 슬롯 그룹: 식당(1), 카페(2), 관광/자연/액티비티/쇼핑(3~6). 숙박(7)은 어느 그룹에도 속하지 않는다. */
    private enum RequiredSlotGroup {
        RESTAURANT("식당", List.of(1)),
        CAFE("카페", List.of(2)),
        EXPERIENCE("관광/자연/액티비티/쇼핑", List.of(3, 4, 5, 6));

        private final String displayName;
        private final List<Integer> categoryCodes;

        RequiredSlotGroup(String displayName, List<Integer> categoryCodes) {
            this.displayName = displayName;
            this.categoryCodes = categoryCodes;
        }

        boolean matches(Integer category) {
            return category != null && categoryCodes.contains(category);
        }

        static Optional<RequiredSlotGroup> groupOf(Integer category) {
            return Arrays.stream(values()).filter(group -> group.matches(category)).findFirst();
        }
    }

    private boolean isClosedOn(TouristSpot spot, String weekdayLabel) {
        String restdate = spot.getRestdate();
        return restdate != null && restdate.contains(weekdayLabel);
    }

    /** anchor(있으면 mustVisit)에서 시작해 가장 가까운 지점을 그리디하게 이어 붙인 동선 순서를 만든다. */
    private List<TouristSpot> orderByProximity(List<TouristSpot> spots, TouristSpot anchor) {
        List<TouristSpot> remaining = new ArrayList<>(spots);
        TouristSpot current = anchor != null ? anchor : remaining.remove(0);
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
