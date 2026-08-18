package com.sodosiro.domain.course.service.dto;

import com.sodosiro.domain.travel.entity.TouristSpot;
import java.util.List;
import java.util.Map;

public record CandidatePoolResult(List<DayCandidatePool> dayPools, List<DaySlotNeeds> slotNeeds, Map<Long, TouristSpot> byId) {
}
