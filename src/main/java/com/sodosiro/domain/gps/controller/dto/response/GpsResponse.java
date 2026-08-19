package com.sodosiro.domain.gps.controller.dto.response;

import com.sodosiro.domain.gps.entity.Gps;
import java.time.LocalDateTime;

public record GpsResponse(
        Long gpsVerificationId,
        Long courseId,
        Long contentId,
        Integer day,
        LocalDateTime verifiedAt
) {
    public static GpsResponse from(Gps gps) {
        return new GpsResponse(
                gps.getId(),
                gps.getCourseId(),
                gps.getContentId(),
                gps.getDay(),
                gps.getVerifiedAt());
    }
}
