package com.sodosiro.domain.travel.controller.dto;

import com.sodosiro.domain.travel.entity.SpotImage;
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
        List<String> images
) {
    public static TouristSpotDetailResponse from(TouristSpot spot) {
        return new TouristSpotDetailResponse(
                spot.getContentId(), spot.getTitle(), spot.getCategory(), spot.getAddr1(), spot.getAddr2(),
                spot.getFirstImage(), spot.getMapX(), spot.getMapY(), spot.getHomepage(), spot.getOverview(),
                spot.getInfocenter(), spot.getUsetime(), spot.getRestdate(), spot.getParking(),
                spot.getLikeCount(), spot.getAvgRating(), spot.getReviewCount(),
                spot.getImages().stream().map(SpotImage::getImageUrl).toList()
        );
    }
}
