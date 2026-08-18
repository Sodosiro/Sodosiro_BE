package com.sodosiro.domain.travel.docs;

import com.sodosiro.domain.travel.controller.dto.CursorPageResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotDetailResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotSummaryResponse;
import com.sodosiro.domain.travel.controller.dto.TravelSpotSort;
import com.sodosiro.domain.travel.controller.dto.TrendingKeywordResponse;
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
            description = "sort=ALL 또는 DEFAULT는 TourAPI 여행지를 contentId 내림차순으로 전체 조회합니다. "
                    + "sort=POPULAR는 ETL이 계산한 카테고리별 상위 10위 여행지를 종합 인기도순으로 반환합니다. "
                    + "category를 생략하면 전체, 반복 전달하면 여러 카테고리를 조회하며, "
                    + "keyword는 여행지 제목 LIKE 검색에 함께 적용됩니다. 목록의 overview는 최대 30자 미리보기이며, "
                    + "restdate와 인기 여부(isPopular), 인기 태그(popularity.rankTag)를 함께 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "size 또는 cursor가 유효하지 않음")
    })
    ResponseEntity<CursorPageResponse<TouristSpotSummaryResponse>> getTouristSpots(
            @Parameter(in = ParameterIn.QUERY, description = "직전 응답의 nextCursor. sort별 커서는 서로 호환되지 않음", example = "126508") String cursor,
            @Parameter(in = ParameterIn.QUERY, description = "페이지 크기 (기본 20, 최대 10000)", example = "10000") Integer size,
            @Parameter(in = ParameterIn.QUERY, description = "서비스 카테고리. 반복 전달 가능", example = "4") List<Integer> categories,
            @Parameter(in = ParameterIn.QUERY, description = "여행지명 부분 검색어", example = "강릉") String keyword,
            @Parameter(in = ParameterIn.QUERY, description = "조회 정렬 기준", example = "POPULAR",
                    schema = @Schema(type = "string", allowableValues = {"ALL", "DEFAULT", "POPULAR"})) TravelSpotSort sort,
            @Parameter(hidden = true) @LoginUser Long userId
    );

    @Operation(
            summary = "일반 여행지 상세 조회",
            description = "여행지 기본 정보, 좋아요 수, 리뷰 평균 평점, 리뷰 수, 인기 태그, 임베딩·리뷰·좋아요·거리로 보정한 연관 관광지 최대 5건, 공통 ReviewResponse 형식의 최신 리뷰 최대 3건과 detailImage2 이미지 목록을 함께 반환합니다. 연관 관광지는 최초 조회 후 24시간 캐시됩니다. infocenter는 원문에서 추출한 전화번호만 슬래시(/)로 연결해 반환합니다. 최신 리뷰에는 작성자·여행지·방문 유형·GPS 인증·내 리뷰 여부를 모두 포함합니다. "
                    + "리뷰가 없으면 latestReviews 필드는 응답에서 제외됩니다. "
                    + "aiRecommendation.available이 false이면 아직 추천 이유가 생성되지 않았거나 만료된 상태입니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "여행지를 찾을 수 없음")
    })
    ResponseEntity<TouristSpotDetailResponse> getTouristSpotDetail(
            @Parameter(description = "TourAPI 콘텐츠 ID", required = true, example = "126508") Long contentId,
            @Parameter(hidden = true) @LoginUser Long userId
    );

    @Operation(
            summary = "여행지 AI 추천 이유 생성",
            description = "유효한 24시간 캐시가 있으면 즉시 반환하고, 없거나 만료됐을 때만 최신 리뷰를 반영해 추천 이유를 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "캐시된 또는 새로 생성된 추천 이유 반환"),
            @ApiResponse(responseCode = "404", description = "여행지를 찾을 수 없음")
    })
    ResponseEntity<TouristSpotDetailResponse.AiRecommendation> generateAiRecommendation(
            @Parameter(description = "TourAPI 콘텐츠 ID", required = true, example = "126508") Long contentId
    );

    @Operation(
            summary = "인기 검색어 조회",
            description = "최근 30일 누적 검색 횟수를 합산해 상위 10개 인기 검색어를 rank 오름차순으로 반환합니다. "
                    + "여행지 검색(GET /spots?keyword=...) 요청 시 검색어가 Redis Sorted Set에 비동기로 집계되며, "
                    + "조회는 Redis에서 수행됩니다. 누적된 검색어가 없으면 빈 배열을 반환합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공 (rank 오름차순, 최대 10개)")
    })
    ResponseEntity<List<TrendingKeywordResponse>> getSearchTrending();
}
