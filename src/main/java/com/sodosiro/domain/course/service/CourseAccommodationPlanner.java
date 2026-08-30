package com.sodosiro.domain.course.service;

import com.sodosiro.domain.course.entity.Course;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * 확정된 일자별 스팟 구성(days) 마지막에 숙박을 붙인다. AI 없이 같은 시군구 평점순 후보를 사용한다.
 * 당일치기(1일)는 숙박이 필요 없고, 마지막 날은 복귀일이라 숙박을 쓰지 않으므로 제외한다.
 * 후보를 밤 수만큼 순서대로 배정해 밤마다 다른 숙소가 걸리게 하고, 후보가 모자라면 앞에서부터 다시 순환한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class CourseAccommodationPlanner {

    /** TouristSpot.category 중 숙박(7). TravelStyle은 하루 5슬롯 랜덤풀 대상이라 숙박을 의도적으로 제외하므로 여기서 직접 참조한다. */
    private static final int ACCOMMODATION_CATEGORY = 7;
    private static final int CANDIDATE_BUFFER = 3;
    private static final int CANDIDATE_LIMIT_CAP = 100;

    private final TouristSpotRepository touristSpotRepository;

    List<Course.DaySnapshot> attach(List<Course.DaySnapshot> days, String sigunguCode) {
        int nightsNeeded = days.size() - 1;
        if (nightsNeeded <= 0) {
            return days;
        }

        List<TouristSpot> candidates = touristSpotRepository.findByCategoryInAndLdongSignguCodeOrderByAvgRatingDesc(
                List.of(ACCOMMODATION_CATEGORY), sigunguCode, PageRequest.of(0, candidateLimit(nightsNeeded)));

        if (candidates.isEmpty()) {
            log.warn("숙박 후보가 없어 배정을 건너뜁니다. sigunguCode={}", sigunguCode);
            return days;
        }

        List<Course.DaySnapshot> result = new ArrayList<>(days.size());
        int lastDayIndex = days.size() - 1;

        for (int i = 0; i < days.size(); i++) {
            Course.DaySnapshot day = days.get(i);
            if (i == lastDayIndex) {
                result.add(day);
                continue;
            }
            TouristSpot accommodation = candidates.get(i % candidates.size());
            List<Course.SpotSnapshot> spots = new ArrayList<>(day.spots());
            spots.add(toSpotSnapshot(accommodation));
            result.add(new Course.DaySnapshot(day.day(), day.date(), spots));
        }
        return result;
    }

    private Course.SpotSnapshot toSpotSnapshot(TouristSpot spot) {
        return new Course.SpotSnapshot(
                spot.getContentId(), spot.getTitle(), spot.getFirstImage(),
                spot.getMapX(), spot.getMapY(), spot.getCategory(), false);
    }

    private int candidateLimit(int nightsNeeded) {
        return Math.min(CANDIDATE_LIMIT_CAP, nightsNeeded * CANDIDATE_BUFFER);
    }
}
