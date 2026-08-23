package com.sodosiro.domain.region.controller.specification;

import com.sodosiro.domain.region.controller.dto.AreaCodeResponse;
import com.sodosiro.domain.region.controller.dto.RegionCodeResponse;
import com.sodosiro.domain.region.controller.dto.RegionIntroductionResponse;
import com.sodosiro.domain.region.controller.dto.VisitedRegionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "지역", description = "시도·시군구 코드와 시군구별 지역 소개 조회 API")
public interface RegionIntroSpecification {

    @Operation(
            summary = "시도 코드 목록 조회",
            description = "지역 선택 화면의 첫 단계에서 사용하는 법정동 시도 코드 목록입니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    ResponseEntity<List<AreaCodeResponse>> getAreas();

    @Operation(
            summary = "시군구 코드 목록 조회",
            description = "선택한 시도의 시군구 목록을 반환합니다. introductionAvailable이 true인 항목만 지역 소개 화면을 조회할 수 있습니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "400", description = "areaCode가 누락되었거나 형식이 올바르지 않음")
    })
    ResponseEntity<List<RegionCodeResponse>> getRegions(
            @Parameter(in = ParameterIn.QUERY, required = true,
                    description = "법정동 시도 코드. 강원특별자치도는 51", example = "51") String areaCode
    );

    @Operation(
            summary = "시군구 지역 소개 조회",
            description = "소개 문구, 테마·음식 태그, 추천 이유, 방문 추천 시기, 대표 명소와 지역 이미지 최대 10장을 반환합니다. "
                    + "이미지는 해당 시군구의 관광지에서 관광지당 대표 이미지 한 장씩 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "404", description = "지역 소개 또는 시군구 코드를 찾을 수 없음")
    })
    ResponseEntity<RegionIntroductionResponse> getIntroduction(
            @Parameter(in = ParameterIn.PATH, required = true,
                    description = "시군구 코드 목록 API가 반환한 내부 식별자", example = "18") Long sigunguId
    );

    @Operation(
            summary = "내가 실제로 방문한 지역 조회",
            description = "GPS 방문 인증(POST /api/v1/gps)이 완료된 관광지를 기준으로, 로그인 사용자가 실제로 방문한 시군구만 반환합니다. "
                    + "일정에 있었지만 GPS 인증을 하지 않은 관광지는 집계되지 않습니다. "
                    + "areaCode를 생략하면 강원특별자치도(51) 기준으로 조회합니다(현재 이 서비스가 다루는 지역이 강원도뿐이라 기본값으로 둠). "
                    + "visitCount는 해당 시군구에서 GPS 인증된 서로 다른 관광지 수입니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공 (방문 기록이 없으면 visitedSigungus는 빈 배열)")
    ResponseEntity<VisitedRegionResponse> getVisitedRegions(
            @Parameter(hidden = true) Long userId,
            @Parameter(in = ParameterIn.QUERY, description = "법정동 시도 코드. 생략하면 강원특별자치도(51)", example = "51") String areaCode
    );
}
