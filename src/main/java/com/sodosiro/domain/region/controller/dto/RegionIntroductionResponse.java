package com.sodosiro.domain.region.controller.dto;

import java.util.List;
import java.util.Map;

public record RegionIntroductionResponse(
        Long sigunguId,
        String areaCode,
        String sigunguCode,
        String displayName,
        String intro,
        List<String> themeTags,
        List<String> recommendationReasons,
        Map<String, Object> bestSeason,
        List<String> foodTags,
        List<RegionImage> images,
        List<FeaturedSpot> featuredSpots
) {
    public record RegionImage(
            Long contentId,
            String title,
            String imageUrl
    ) {
    }

    public record FeaturedSpot(
            Long contentId,
            String title,
            String address,
            String imageUrl
    ) {
    }
}
