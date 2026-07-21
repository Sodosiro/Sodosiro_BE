package com.sodosiro.domain.dataextract.controller.dto;

import java.util.List;

public record TravelRefreshRequest(String runId, List<Long> contentIds) {
}
