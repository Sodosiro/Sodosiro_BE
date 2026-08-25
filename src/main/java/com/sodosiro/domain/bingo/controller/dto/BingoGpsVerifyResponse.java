package com.sodosiro.domain.bingo.controller.dto;

import com.sodosiro.domain.gps.entity.Gps;
import java.time.LocalDateTime;

public record BingoGpsVerifyResponse(
        Long gpsVerificationId,
        Long contentId,
        LocalDateTime verifiedAt,
        BingoCellCheckResponse bingoCheck
) {
    /** bingoCheck는 이 관광지가 활성 시즌 빙고판의 칸일 때만 채워지고, 아니면 null이다. */
    public static BingoGpsVerifyResponse from(Gps gps, BingoCellCheckResponse bingoCheck) {
        return new BingoGpsVerifyResponse(
                gps.getId(),
                gps.getContentId(),
                gps.getVerifiedAt(),
                bingoCheck);
    }
}
