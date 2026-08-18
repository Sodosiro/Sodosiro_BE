package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.constants.TravelStyle;
import com.sodosiro.domain.travel.entity.TouristSpot;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * AI 경로({@link CourseAiPlanner})와 규칙기반 경로({@link CourseRuleBasedPlanner})가 공통으로 쓰는
 * 식당 자리 강제 배치 로직. 어떤 스팟을 넣을지/식당이 아닌 스팟들 사이의 상대 순서는 호출자가 이미 정한 그대로 두고,
 * 식당만 점심·저녁 고정 자리({@link #RESTAURANT_SLOT_POSITIONS})로 옮긴다.
 * 규칙기반 폴백은 후보 부족 시 하루 5곳/식당 2곳 계약이 깨질 수 있으므로, 식당이 0~2곳이거나 목록이
 * 5곳보다 짧아도 안전하게 동작한다: 실제 존재하는 자리(0, 3)에만, 실제 존재하는 식당 수만큼만 고정하고
 * 자리를 못 받은 식당(후보가 모자라 목록이 짧아진 경우)은 원래 순서 그대로 나머지 스팟들 사이에 남는다.
 */
final class MealSlotOrdering {

    /** 5슬롯 기준 식당(점심·저녁) 고정 자리: 1번째(0)·4번째(3). */
    private static final Set<Integer> RESTAURANT_SLOT_POSITIONS = Set.of(0, 3);

    private MealSlotOrdering() {
    }

    static List<Long> placeRestaurantsAtMealSlots(List<Long> contentIds, Map<Long, TouristSpot> byId) {
        List<Long> restaurants = contentIds.stream().filter(contentId -> isRestaurant(byId.get(contentId))).toList();

        List<Integer> targetPositions = RESTAURANT_SLOT_POSITIONS.stream()
                .filter(position -> position < contentIds.size())
                .sorted()
                .toList();
        int fixedCount = Math.min(restaurants.size(), targetPositions.size());
        Set<Long> fixedRestaurantIds = new HashSet<>(restaurants.subList(0, fixedCount));
        Set<Integer> fixedPositions = new HashSet<>(targetPositions.subList(0, fixedCount));

        //자리를 못 받은 식당(목록이 짧아 0/3번 자리가 없거나, 필요한 만큼 식당이 없는 경우)은 원래 순서 그대로 남긴다.
        List<Long> remainder = contentIds.stream().filter(contentId -> !fixedRestaurantIds.contains(contentId)).toList();

        List<Long> ordered = new ArrayList<>(contentIds.size());
        int fixedIndex = 0;
        int remainderIndex = 0;
        for (int position = 0; position < contentIds.size(); position++) {
            ordered.add(fixedPositions.contains(position)
                    ? restaurants.get(fixedIndex++)
                    : remainder.get(remainderIndex++));
        }
        return ordered;
    }

    private static boolean isRestaurant(TouristSpot spot) {
        return Objects.equals(spot.getCategory(), TravelStyle.RESTAURANT.categoryCode());
    }
}
