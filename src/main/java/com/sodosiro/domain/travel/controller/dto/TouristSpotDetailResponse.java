package com.sodosiro.domain.travel.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sodosiro.domain.review.controller.dto.response.ReviewResponse;
import com.sodosiro.domain.travel.entity.SpotImage;
import com.sodosiro.domain.travel.entity.SpotPopularity;
import com.sodosiro.domain.travel.entity.TouristSpot;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public record TouristSpotDetailResponse(
        Long contentId,
        String title,
        Integer category,
        String addr1,
        String addr2,
        String firstImage,
        BigDecimal mapX,
        BigDecimal mapY,
        String homepage,
        String overview,
        String infocenter,
        String usetime,
        String restdate,
        String parking,
        Integer likeCount,
        boolean liked,
        @Schema(description = "리뷰 평균 별점 (소수점 한 자리)", example = "4.5")
        BigDecimal avgRating,
        Integer reviewCount,
        Popularity popularity,
        AiRecommendation aiRecommendation,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<TouristSpotSummaryResponse> relatedSpots,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<ReviewResponse> latestReviews,
        List<String> images
) {
    public static TouristSpotDetailResponse from(
            TouristSpot spot, Popularity popularity, AiRecommendation aiRecommendation,
            List<TouristSpotSummaryResponse> relatedSpots, List<ReviewResponse> latestReviews, boolean liked) {
        return new TouristSpotDetailResponse(
                spot.getContentId(), spot.getTitle(), spot.getCategory(), spot.getAddr1(), spot.getAddr2(),
                spot.getFirstImage(), spot.getMapX(), spot.getMapY(), spot.getHomepage(), spot.getOverview(),
                InfoCenterPhoneParser.extract(spot.getInfocenter()), spot.getUsetime(), spot.getRestdate(), spot.getParking(),
                spot.getLikeCount(), liked, spot.getAvgRating().setScale(1, RoundingMode.HALF_UP), spot.getReviewCount(), popularity, aiRecommendation,
                relatedSpots.isEmpty() ? null : relatedSpots,
                latestReviews.isEmpty() ? null : latestReviews,
                spot.getImages().stream().map(SpotImage::getImageUrl).toList()
        );
    }

    public record AiRecommendation(boolean available, String reason) {
        public static AiRecommendation unavailable() {
            return new AiRecommendation(false, null);
        }

        public static AiRecommendation available(String reason) {
            return new AiRecommendation(true, reason);
        }
    }

    public record Popularity(String rankTag, Integer categoryRank) {
        public static Popularity from(SpotPopularity popularity) {
            if (popularity == null || popularity.getRankTag() == null) {
                return null;
            }
            return new Popularity(popularity.getRankTag(), popularity.getCategoryRank());
        }
    }

}
