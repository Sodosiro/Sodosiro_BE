package com.sodosiro.domain.region.docs;

import com.sodosiro.domain.region.controller.dto.AreaCodeResponse;
import com.sodosiro.domain.region.controller.dto.RegionCodeResponse;
import com.sodosiro.domain.region.controller.dto.RegionIntroductionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;

@Tag(name = "Region", description = "시도·시군구 코드와 시군구별 지역 소개 조회 API")
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
}
