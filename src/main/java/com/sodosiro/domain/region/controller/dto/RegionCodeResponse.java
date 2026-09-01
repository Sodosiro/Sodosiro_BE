package com.sodosiro.domain.region.controller.dto;

import com.sodosiro.domain.travel.entity.SigunguCode;

public record RegionCodeResponse(
        Long sigunguId,
        String areaCode,
        String sigunguCode,
        String thumbnailUrl,
        String name
) {
    public static RegionCodeResponse from(SigunguCode sigungu) {
        return new RegionCodeResponse(
                sigungu.getId(), sigungu.getAreaCode(), sigungu.getSigunguCode(),
                sigungu.getThumbnailUrl(), sigungu.getName());
    }
}
