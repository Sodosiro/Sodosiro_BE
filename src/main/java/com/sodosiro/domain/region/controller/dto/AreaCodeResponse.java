package com.sodosiro.domain.region.controller.dto;

import com.sodosiro.domain.travel.entity.AreaCode;

public record AreaCodeResponse(String areaCode, String name) {

    public static AreaCodeResponse from(AreaCode area) {
        return new AreaCodeResponse(area.getAreaCode(), area.getName());
    }
}
