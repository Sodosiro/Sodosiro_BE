package com.sodosiro.domain.bingo.controller.specification;

import com.sodosiro.domain.bingo.constants.SeasonType;
import com.sodosiro.domain.bingo.controller.dto.BingoBoardResponse;
import com.sodosiro.domain.bingo.controller.dto.BingoGpsRequest;
import com.sodosiro.domain.bingo.controller.dto.BingoGpsVerifyResponse;
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

    @Operation(summary = "빙고 전용 GPS 방문 인증",
            description = "코스/일정과 무관하게 관광지 하나를 지정해 현재 GPS 좌표와의 거리가 300m 이내이면 인증 레코드를 새로 생성합니다. "
                    + "300m 밖이면 레코드를 만들지 않고 오류를 반환하며, 이미 인증된 스팟이면 기존 인증 결과를 그대로 반환합니다. "
                    + "여기서 만든 인증은 코스 GPS 인증(/api/v1/gps)과 동일한 기록을 공유하므로 어느 쪽에서 인증하든 빙고판에 동일하게 반영됩니다. "
                    + "원본 GPS 좌표는 저장하지 않습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "인증 성공 (신규 생성 또는 기존 인증 반환)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 관광지"),
            @ApiResponse(responseCode = "409", description = "관광지 위치 정보 없음 또는 반경 300m 밖")
    })
    ResponseEntity<BingoGpsVerifyResponse> verifyGps(
            @Parameter(hidden = true) Long userId,
            BingoGpsRequest request);
}
