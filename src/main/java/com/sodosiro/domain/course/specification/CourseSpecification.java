package com.sodosiro.domain.course.specification;

import com.sodosiro.domain.course.controller.dto.CourseRecommendRequest;
import com.sodosiro.domain.course.controller.dto.CourseRecommendResponse;
import com.sodosiro.global.resolver.LoginUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface CourseSpecification {


    @Operation(
            summary = "AI 코스 추천 생성",
            description = "사용자의 입력 조건을 바탕으로 맞춤형 여행 코스를 추천합니다. "
                    + "여행 기간, 선호 카테고리, 특정 여행지 포함 여부 등을 반영하여 최적의 코스를 구성하며, "
                    + "로그인한 사용자의 고유 식별자(userId)를 기반으로 코스를 생성합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코스 추천 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 유효성 검증 실패 (예: 필수값 누락, 일자 범위 오류 등)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    public ResponseEntity<CourseRecommendResponse> recommend(
            @Parameter(hidden = true) @LoginUser Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "코스 추천에 필요한 요청 정보 (여행 일정, 선호 태그, 출발지 등)",
                    required = true
            )
            @RequestBody @Valid CourseRecommendRequest request
    );
}
