package com.sodosiro.domain.bingo.controller.specification;

import com.sodosiro.domain.bingo.constants.SeasonType;
import com.sodosiro.domain.bingo.controller.dto.BingoBoardResponse;
import com.sodosiro.domain.bingo.controller.dto.BingoSeasonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.springframework.http.ResponseEntity;

public interface BingoSpecification {

    @Operation(summary = "빙고 시즌 목록 조회",
            description = "지금까지 쌓인 빙고 시즌 전체를 시작일 최신순으로 반환합니다 (예: 2026 봄, 2025 겨울, 2025 가을 ...). "
                    + "지역 빙고판을 지난 시즌 기준으로 조회할 때 여기서 얻은 year/seasonType 조합을 사용합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    ResponseEntity<List<BingoSeasonResponse>> listSeasons();

    @Operation(summary = "지역 빙고판 조회",
            description = "해당 지역의 빙고판 9칸과, 로그인한 사용자의 칸별 GPS 인증 달성 여부·인증 시각·완성 라인 수를 반환합니다. "
                    + "빙고판 내용은 시즌 동안 모든 사용자에게 동일합니다. "
                    + "year/seasonType을 둘 다 생략하면 진행 중인 시즌을, 둘 다 주면 그 시즌(지난 시즌 포함)을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "year/seasonType 중 하나만 전달됨"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "조회 조건에 맞는 시즌이 없거나 해당 지역의 빙고판이 없음")
    })
    ResponseEntity<BingoBoardResponse> getBoard(
            @Parameter(hidden = true) Long userId,
            @Parameter(description = "지역 ID (region_intro sigunguId)") Long sigunguId,
            @Parameter(description = "조회할 시즌 연도 (seasonType과 함께 지정, 생략 시 활성 시즌)") Integer year,
            @Parameter(description = "조회할 계절 구분 (year와 함께 지정, 생략 시 활성 시즌)") SeasonType seasonType);
}
