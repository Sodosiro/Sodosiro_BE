package com.sodosiro.domain.travel.docs;

import com.sodosiro.domain.travel.controller.dto.CursorPageResponse;
import com.sodosiro.domain.travel.controller.dto.KakaoSpotResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotDetailResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotSummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Travel", description = "일반 여행지와 카카오 ETL 기반 인기 장소 조회 API")
public interface TravelSpotSpecification {

    @Operation(
            summary = "일반 여행지 목록 조회",
            description = "TourAPI 여행지를 contentId 내림차순 커서 방식으로 조회합니다. "
                    + "category를 생략하면 전체, 반복 전달하면 여러 카테고리를 조회하며, "
                    + "keyword는 여행지 제목 LIKE 검색에 함께 적용됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "size 또는 cursor가 유효하지 않음")
    })
    ResponseEntity<CursorPageResponse<TouristSpotSummaryResponse>> getTouristSpots(
            @Parameter(in = ParameterIn.QUERY, description = "직전 응답의 nextCursor", example = "126508") String cursor,
            @Parameter(in = ParameterIn.QUERY, description = "페이지 크기 (기본 20, 최대 100)", example = "20") Integer size,
            @Parameter(in = ParameterIn.QUERY, description = "서비스 카테고리. 반복 전달 가능", example = "4") List<Integer> categories,
            @Parameter(in = ParameterIn.QUERY, description = "여행지명 부분 검색어", example = "강릉") String keyword
    );

    @Operation(
            summary = "일반 여행지 상세 조회",
            description = "여행지 기본 정보와 detailImage2 이미지 목록을 함께 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "여행지를 찾을 수 없음")
    })
    ResponseEntity<TouristSpotDetailResponse> getTouristSpotDetail(
            @Parameter(description = "TourAPI 콘텐츠 ID", required = true, example = "126508") Long contentId
    );

    @Operation(
            summary = "카카오 인기 장소 목록 조회",
            description = "ETL이 수집한 카카오 장소를 popularityScore 내림차순으로 조회합니다. "
                    + "동점 점수에서도 누락·중복이 없도록 복합 커서를 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "size 또는 cursor가 유효하지 않음")
    })
    ResponseEntity<CursorPageResponse<KakaoSpotResponse>> getPopularSpots(
            @Parameter(in = ParameterIn.QUERY, description = "직전 응답의 nextCursor", example = "MS4wOjEyMw") String cursor,
            @Parameter(in = ParameterIn.QUERY, description = "페이지 크기 (기본 20, 최대 100)", example = "20") Integer size,
            @Parameter(in = ParameterIn.QUERY, description = "카카오 카테고리 그룹 코드. 반복 전달 가능", example = "AT4",
                    schema = @Schema(type = "string", allowableValues = {"AT4", "AD5", "FD6", "CE7"}))
            List<String> categoryGroupCodes
    );
}
