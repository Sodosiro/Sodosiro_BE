package com.sodosiro.domain.course.service.dto;

import java.util.List;
import java.util.Map;

public record DayCandidatePool(
        int day, String date, String weekday,
        List<CandidateSpot> restaurants, Map<Integer, List<CandidateSpot>> styleCandidates,
        List<CandidateSpot> randomPool, CandidateSpot mustVisit) {
}
