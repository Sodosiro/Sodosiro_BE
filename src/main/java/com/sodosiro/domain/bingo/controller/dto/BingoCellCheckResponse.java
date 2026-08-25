package com.sodosiro.domain.bingo.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/** GPS 인증 직후 붙는 가벼운 빙고 표시. 9칸 전체가 아니라 이번에 인증한 칸의 결과만 담는다. */
public record BingoCellCheckResponse(
        @Schema(description = "빙고판 ID") Long boardId,
        @Schema(description = "이번에 인증한 관광지의 칸 위치 1~9") int position,
        @Schema(description = "완성된 라인 수 (가로3+세로3+대각2 중)") int completedLineCount
) {
}
