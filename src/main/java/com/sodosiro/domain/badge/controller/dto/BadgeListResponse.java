package com.sodosiro.domain.badge.controller.dto;

import java.time.LocalDateTime;
import java.util.List;

public record BadgeListResponse(
        int collectedCount,
        int totalCount,
        List<BadgeItem> badges
) {
    public record BadgeItem(
            Long badgeId,
            String name,
            boolean earned,
            LocalDateTime earnedAt
    ) {
    }
}
