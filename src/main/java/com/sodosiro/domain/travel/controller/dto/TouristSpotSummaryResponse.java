package com.sodosiro.domain.travel.controller.dto;

import com.sodosiro.domain.travel.entity.TouristSpot;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TouristSpotSummaryResponse(
        Long contentId,
        String title,
        Integer category,
        String addr1,
        String firstImage,
        BigDecimal mapX,
        BigDecimal mapY,
        Integer likeCount,
        BigDecimal avgRating,
        Integer reviewCount,
        boolean liked,
        Popularity popularity
) {
    public static TouristSpotSummaryResponse from(TouristSpot spot) {
        return new TouristSpotSummaryResponse(
                spot.getContentId(), spot.getTitle(), spot.getCategory(), spot.getAddr1(),
                spot.getFirstImage(), spot.getMapX(), spot.getMapY(),
                spot.getLikeCount(), spot.getAvgRating(), spot.getReviewCount(), false, null
        );
    }

    public static TouristSpotSummaryResponse from(
            TouristSpot spot, Popularity popularity, boolean liked) {
        return new TouristSpotSummaryResponse(
                spot.getContentId(), spot.getTitle(), spot.getCategory(), spot.getAddr1(),
                spot.getFirstImage(), spot.getMapX(), spot.getMapY(),
                spot.getLikeCount(), spot.getAvgRating(), spot.getReviewCount(), liked, popularity
        );
    }

    public record Popularity(
            Double score,
            Integer categoryRank,
            String rankTag,
            LocalDateTime calculatedAt
    ) {
    }
}
