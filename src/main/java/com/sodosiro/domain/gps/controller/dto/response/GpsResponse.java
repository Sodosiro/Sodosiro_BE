package com.sodosiro.domain.gps.controller.dto.response;

import com.sodosiro.domain.bingo.controller.dto.BingoCellCheckResponse;
import com.sodosiro.domain.gps.entity.Gps;
import java.time.LocalDateTime;

public record GpsResponse(
        Long gpsVerificationId,
        Long courseId,
        Long contentId,
        Integer day,
        LocalDateTime verifiedAt,
        BingoCellCheckResponse bingoCheck
) {
    /** bingoCheck는 이 관광지가 활성 시즌 빙고판의 칸일 때만 채워지고, 아니면 null이다. */
    public static GpsResponse from(Gps gps, BingoCellCheckResponse bingoCheck) {
        return new GpsResponse(
                gps.getId(),
                gps.getCourseId(),
                gps.getContentId(),
                gps.getDay(),
                gps.getVerifiedAt(),
                bingoCheck);
    }
}
