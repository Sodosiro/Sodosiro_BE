package com.sodosiro.domain.course.service.dto;

import com.sodosiro.domain.travel.entity.TouristSpot;
import java.math.BigDecimal;

public record CandidateSpot(Long id, String title, Integer category, BigDecimal mapX, BigDecimal mapY, BigDecimal avgRating) {

    public static CandidateSpot from(TouristSpot spot) {
        return new CandidateSpot(
                spot.getContentId(), spot.getTitle(), spot.getCategory(),
                spot.getMapX(), spot.getMapY(), spot.getAvgRating());
    }
}
