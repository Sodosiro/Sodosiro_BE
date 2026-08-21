package com.sodosiro.domain.festival.docs;

import com.sodosiro.domain.festival.controller.dto.FestivalDetailResponse;
import com.sodosiro.domain.festival.controller.dto.FestivalStatus;
import com.sodosiro.domain.festival.controller.dto.FestivalSummaryResponse;
import com.sodosiro.domain.travel.controller.dto.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "축제", description = "지역별 축제 조회 API")
public interface FestivalSpecification {

    @Operation(
            summary = "지역별 축제 목록 조회",
            description = "축제를 시작일 오름차순(시작일이 같으면 festivalId 오름차순)으로 조회합니다. "
                    + "areaCode를 생략하면 전국, 지정하면 해당 광역시도 축제만 반환합니다. "
                    + "status는 한국 시간대(KST, UTC+9) 기준 오늘 날짜로 판정하며, "
                    + "ONGOING(진행중)·UPCOMING(예정)·ACTIVE(진행중+예정, 아직 끝나지 않음)·ENDED(종료)·ALL(전체) 중 하나입니다. "
                    + "각 축제 응답의 status 필드도 동일하게 KST 기준으로 계산됩니다. "
                    + "year를 지정하면 축제 기간이 해당 연도와 하루라도 겹치는 축제만 반환합니다(생략 시 연도 무관)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "size 또는 cursor가 유효하지 않음")
    })
    ResponseEntity<CursorPageResponse<FestivalSummaryResponse>> getFestivals(
            @Parameter(in = ParameterIn.QUERY, description = "광역시도 코드. 생략 시 전국", example = "1") String areaCode,
            @Parameter(in = ParameterIn.QUERY, description = "진행 상태 필터 (기본 ALL)", example = "ONGOING",
                    schema = @Schema(type = "string", allowableValues = {"ALL", "ONGOING", "UPCOMING", "ACTIVE", "ENDED"})) FestivalStatus status,
            @Parameter(in = ParameterIn.QUERY, description = "연도 필터. 해당 연도와 기간이 겹치는 축제만 조회. 생략 시 전체", example = "2026") Integer year,
            @Parameter(in = ParameterIn.QUERY, description = "직전 응답의 nextCursor") String cursor,
            @Parameter(in = ParameterIn.QUERY, description = "페이지 크기 (기본 20, 최대 100)", example = "20") Integer size
    );

    @Operation(
            summary = "축제 상세 조회",
            description = "festivalId로 단일 축제를 조회합니다. 목록과 달리 description·tag를 잘라내지 않고 전문을 반환합니다. "
                    + "status는 목록과 동일하게 KST 기준 오늘 날짜로 판정합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 축제")
    })
    ResponseEntity<FestivalDetailResponse> getFestival(
            @Parameter(in = ParameterIn.PATH, description = "축제 ID", example = "1") Long festivalId
    );
}
