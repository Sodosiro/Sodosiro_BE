package com.sodosiro.domain.travel.controller.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.sodosiro.domain.review.controller.dto.response.ReviewResponse;
import com.sodosiro.domain.travel.entity.SpotImage;
import com.sodosiro.domain.travel.entity.SpotPopularity;
import com.sodosiro.domain.travel.entity.TouristSpot;
import java.math.BigDecimal;
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
        BigDecimal avgRating,
        Integer reviewCount,
        Popularity popularity,
        AiRecommendation aiRecommendation,
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        List<ReviewResponse> latestReviews,
        List<String> images
) {
    public static TouristSpotDetailResponse from(
            TouristSpot spot, Popularity popularity, AiRecommendation aiRecommendation,
            List<ReviewResponse> latestReviews) {
        return new TouristSpotDetailResponse(
                spot.getContentId(), spot.getTitle(), spot.getCategory(), spot.getAddr1(), spot.getAddr2(),
                spot.getFirstImage(), spot.getMapX(), spot.getMapY(), spot.getHomepage(), spot.getOverview(),
                InfoCenterPhoneParser.extract(spot.getInfocenter()), spot.getUsetime(), spot.getRestdate(), spot.getParking(),
                spot.getLikeCount(), spot.getAvgRating(), spot.getReviewCount(), popularity, aiRecommendation,
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
