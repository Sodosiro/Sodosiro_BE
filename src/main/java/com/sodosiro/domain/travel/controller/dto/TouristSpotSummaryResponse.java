package com.sodosiro.domain.travel.controller.dto;

import com.sodosiro.domain.travel.entity.TouristSpot;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TouristSpotSummaryResponse(
        Long contentId,
        String title,
        Integer category,
        String addr1,
        String overview,
        String restdate,
        String firstImage,
        BigDecimal mapX,
        BigDecimal mapY,
        Integer likeCount,
        BigDecimal avgRating,
        Integer reviewCount,
        boolean liked,
        boolean isPopular,
        Popularity popularity
) {
    public static TouristSpotSummaryResponse from(TouristSpot spot) {
        return new TouristSpotSummaryResponse(
                spot.getContentId(), spot.getTitle(), spot.getCategory(), spot.getAddr1(),
                abbreviateOverview(spot.getOverview()), spot.getRestdate(),
                spot.getFirstImage(), spot.getMapX(), spot.getMapY(),
                spot.getLikeCount(), spot.getAvgRating(), spot.getReviewCount(), false, false, null
        );
    }

    public static TouristSpotSummaryResponse from(
            TouristSpot spot, Popularity popularity, boolean liked) {
        return new TouristSpotSummaryResponse(
                spot.getContentId(), spot.getTitle(), spot.getCategory(), spot.getAddr1(),
                abbreviateOverview(spot.getOverview()), spot.getRestdate(),
                spot.getFirstImage(), spot.getMapX(), spot.getMapY(),
                spot.getLikeCount(), spot.getAvgRating(), spot.getReviewCount(), liked,
                isPopular(popularity), popularity
        );
    }

    static boolean isPopular(Popularity popularity) {
        return popularity != null && popularity.rankTag() != null && !popularity.rankTag().isBlank();
    }

    static String abbreviateOverview(String overview) {
        if (overview == null || overview.length() <= 30) {
            return overview;
        }
        return overview.substring(0, 30) + "...";
    }

    public record Popularity(
            Double score,
            Integer categoryRank,
            String rankTag,
            LocalDateTime calculatedAt
    ) {
    }
}
