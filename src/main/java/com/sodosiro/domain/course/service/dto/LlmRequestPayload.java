package com.sodosiro.domain.course.service.dto;

import java.util.List;

public record LlmRequestPayload(int tripDays, List<DayCandidatePool> days) {
}
