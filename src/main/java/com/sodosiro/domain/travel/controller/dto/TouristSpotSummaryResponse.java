package com.sodosiro.domain.travel.controller.dto;

import com.sodosiro.domain.travel.entity.TouristSpot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public record TouristSpotSummaryResponse(
        Long contentId,
        String title,
        Integer category,
        String addr1,
        @Schema(description = "법정동 지역코드로 해석한 시군구명 (시군구 없으면 광역시도명)", example = "원주시")
        String region,
        String overview,
        String restdate,
        String firstImage,
        BigDecimal mapX,
        BigDecimal mapY,
        Integer likeCount,
        @Schema(description = "리뷰 평균 별점 (소수점 한 자리)", example = "4.5")
        BigDecimal avgRating,
        Integer reviewCount,
        boolean liked,
        boolean isPopular,
        Popularity popularity
) {
    public static TouristSpotSummaryResponse from(TouristSpot spot) {
        return new TouristSpotSummaryResponse(
                spot.getContentId(), spot.getTitle(), spot.getCategory(), spot.getAddr1(), null,
                abbreviateOverview(spot.getOverview()), spot.getRestdate(),
                spot.getFirstImage(), spot.getMapX(), spot.getMapY(),
                spot.getLikeCount(), scale1(spot.getAvgRating()), spot.getReviewCount(), false, false, null
        );
    }

    public static TouristSpotSummaryResponse from(
            TouristSpot spot, Popularity popularity, boolean liked, String region) {
        return new TouristSpotSummaryResponse(
                spot.getContentId(), spot.getTitle(), spot.getCategory(), spot.getAddr1(), region,
                abbreviateOverview(spot.getOverview()), spot.getRestdate(),
                spot.getFirstImage(), spot.getMapX(), spot.getMapY(),
                spot.getLikeCount(), scale1(spot.getAvgRating()), spot.getReviewCount(), liked,
                isPopular(popularity), popularity
        );
    }

    private static BigDecimal scale1(BigDecimal value) {
        return value == null ? null : value.setScale(1, RoundingMode.HALF_UP);
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
