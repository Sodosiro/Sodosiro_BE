package com.sodosiro.domain.region.controller.dto;

import com.sodosiro.domain.travel.entity.SigunguCode;

public record RegionCodeResponse(
        Long sigunguId,
        String areaCode,
        String sigunguCode,
        String name,
        boolean introductionAvailable
) {
    public static RegionCodeResponse from(SigunguCode sigungu, boolean introductionAvailable) {
        return new RegionCodeResponse(
                sigungu.getId(), sigungu.getAreaCode(), sigungu.getSigunguCode(),
                sigungu.getName(), introductionAvailable);
    }
}
