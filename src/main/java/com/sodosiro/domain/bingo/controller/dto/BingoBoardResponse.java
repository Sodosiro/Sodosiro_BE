package com.sodosiro.domain.bingo.controller.dto;

import com.sodosiro.domain.bingo.constants.SeasonType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

public record BingoBoardResponse(
        @Schema(description = "빙고판 ID") Long boardId,
        @Schema(description = "시즌 ID") Long seasonId,
        @Schema(description = "시즌 연도") Integer year,
        @Schema(description = "계절 구분") SeasonType seasonType,
        @Schema(description = "지역 ID (region_intro sigunguId)") Long sigunguId,
        @Schema(description = "9칸 (position 1~9)") List<Cell> cells,
        @Schema(description = "완성된 라인 수 (가로3+세로3+대각2 중)") int completedLineCount,
        @Schema(description = "라인 1개 이상 완성 여부") boolean bingoAchieved
) {
    public record Cell(
            @Schema(description = "칸 위치 1~9 (1행 1-3, 2행 4-6, 3행 7-9)") int position,
            @Schema(description = "관광지 contentId") Long contentId,
            @Schema(description = "관광지명") String title,
            @Schema(description = "대표 이미지") String firstImage,
            @Schema(description = "카테고리") Integer category,
            @Schema(description = "GPS 인증으로 달성했는지 여부") boolean completed,
            @Schema(description = "GPS 인증 시각 (미인증이면 null)") LocalDateTime verifiedAt
    ) {
    }
}
