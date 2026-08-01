package com.sodosiro.domain.route.odsay.dto;

import java.util.List;

public record OdsaySegmentResponse(
        String trafficType,
        Integer sectionTimeMinutes,
        Integer distanceMeters,
        List<String> laneNames
) {
}
