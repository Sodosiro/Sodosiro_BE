package com.sodosiro.domain.travel.docs;

import com.sodosiro.domain.travel.controller.dto.CursorPageResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotDetailResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotSummaryResponse;
import com.sodosiro.domain.travel.controller.dto.TravelSpotSort;
import com.sodosiro.global.resolver.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Travel", description = "일반 여행지와 ETL 기반 인기 여행지 조회 API")
public interface TravelSpotSpecification {

    @Operation(
            summary = "여행지 목록 조회",
            description = "sort=DEFAULT는 TourAPI 여행지를 contentId 내림차순으로 조회합니다. "
                    + "sort=POPULAR는 ETL이 계산한 카테고리별 상위 10위 여행지를 종합 인기도순으로 반환합니다. "
                    + "category를 생략하면 전체, 반복 전달하면 여러 카테고리를 조회하며, "
                    + "keyword는 여행지 제목 LIKE 검색에 함께 적용됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "size 또는 cursor가 유효하지 않음")
    })
    ResponseEntity<CursorPageResponse<TouristSpotSummaryResponse>> getTouristSpots(
            @Parameter(in = ParameterIn.QUERY, description = "직전 응답의 nextCursor. sort별 커서는 서로 호환되지 않음", example = "126508") String cursor,
            @Parameter(in = ParameterIn.QUERY, description = "페이지 크기 (기본 20, 최대 100)", example = "20") Integer size,
            @Parameter(in = ParameterIn.QUERY, description = "서비스 카테고리. 반복 전달 가능", example = "4") List<Integer> categories,
            @Parameter(in = ParameterIn.QUERY, description = "여행지명 부분 검색어", example = "강릉") String keyword,
            @Parameter(in = ParameterIn.QUERY, description = "조회 정렬 기준", example = "POPULAR",
                    schema = @Schema(type = "string", allowableValues = {"DEFAULT", "POPULAR"})) TravelSpotSort sort,
            @Parameter(hidden = true) @LoginUser Long userId
    );

    @Operation(
            summary = "일반 여행지 상세 조회",
            description = "여행지 기본 정보, 좋아요 수, 리뷰 평균 평점, 리뷰 수와 detailImage2 이미지 목록을 함께 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "여행지를 찾을 수 없음")
    })
    ResponseEntity<TouristSpotDetailResponse> getTouristSpotDetail(
            @Parameter(description = "TourAPI 콘텐츠 ID", required = true, example = "126508") Long contentId
    );
}
