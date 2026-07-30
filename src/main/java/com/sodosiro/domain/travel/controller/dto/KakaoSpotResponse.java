package com.sodosiro.domain.travel.controller.dto;

import com.sodosiro.domain.travel.entity.KakaoSpot;

public record KakaoSpotResponse(
        Long id,
        String placeName,
        String cityName,
        String addressName,
        String categoryGroupCode,
        String categoryGroupName,
        Double longitude,
        Double latitude,
        Double popularityScore
) {
    public static KakaoSpotResponse from(KakaoSpot spot) {
        return new KakaoSpotResponse(
                spot.getId(), spot.getPlaceName(), spot.getCityName(), spot.getAddressName(),
                spot.getCategoryGroupCode(), spot.getCategoryGroupName(), spot.getLongitude(), spot.getLatitude(),
                spot.getPopularityScore()
        );
    }
}
