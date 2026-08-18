package com.sodosiro.domain.course.service.dto;

import java.util.Map;

/** 그날 각 후보 버킷에서 정확히 몇 개를 뽑아야 하루 슬롯 수가 채워지는지를 담는다. LLM에게는 안 보내고, 규칙기반 선택에서만 쓴다. */
public record DaySlotNeeds(int restaurantNeeded, Map<Integer, Integer> styleNeeded, int randomNeeded) {
}
