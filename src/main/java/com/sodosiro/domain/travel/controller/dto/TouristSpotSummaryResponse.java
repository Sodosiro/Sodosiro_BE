package com.sodosiro.domain.travel.controller.dto;

import com.sodosiro.domain.travel.entity.TouristSpot;
import java.math.BigDecimal;

public record TouristSpotSummaryResponse(
        Long contentId,
        String title,
        Integer category,
        String addr1,
        String firstImage,
        BigDecimal mapX,
        BigDecimal mapY
) {
    public static TouristSpotSummaryResponse from(TouristSpot spot) {
        return new TouristSpotSummaryResponse(
                spot.getContentId(), spot.getTitle(), spot.getCategory(), spot.getAddr1(),
                spot.getFirstImage(), spot.getMapX(), spot.getMapY()
        );
    }
}
