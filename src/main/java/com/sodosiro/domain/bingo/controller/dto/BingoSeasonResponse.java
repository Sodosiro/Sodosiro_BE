package com.sodosiro.domain.bingo.controller.dto;

import com.sodosiro.domain.bingo.constants.BingoSeasonStatus;
import com.sodosiro.domain.bingo.constants.SeasonType;
import com.sodosiro.domain.bingo.entity.BingoSeason;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

public record BingoSeasonResponse(
        @Schema(description = "시즌 연도") Integer year,
        @Schema(description = "계절 구분") SeasonType seasonType,
        @Schema(description = "시즌 상태") BingoSeasonStatus status,
        @Schema(description = "시즌 시작일") LocalDate startDate,
        @Schema(description = "시즌 종료일") LocalDate endDate
) {
    public static BingoSeasonResponse from(BingoSeason season) {
        return new BingoSeasonResponse(
                season.getYear(), season.getSeasonType(), season.getStatus(), season.getStartDate(), season.getEndDate());
    }
}
