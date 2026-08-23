package com.sodosiro.domain.region.controller.dto;

import java.util.List;

public record VisitedRegionResponse(
        String areaCode,
        String areaName,
        List<VisitedSigungu> visitedSigungus
) {
    public record VisitedSigungu(
            Long sigunguId,
            String sigunguCode,
            String name,
            int visitCount
    ) {
    }
}
