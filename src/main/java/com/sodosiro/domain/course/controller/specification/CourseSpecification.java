package com.sodosiro.domain.course.controller.specification;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.controller.dto.CourseConfirmRequest;
import com.sodosiro.domain.course.controller.dto.CourseDayUpdateRequest;
import com.sodosiro.domain.course.controller.dto.CourseDetailResponse;
import com.sodosiro.domain.course.controller.dto.CourseRecommendQuotaResponse;
import com.sodosiro.domain.course.controller.dto.CourseRecommendRequest;
import com.sodosiro.domain.course.controller.dto.CourseRecommendResponse;
import com.sodosiro.global.resolver.LoginUser;
import com.sodosiro.domain.course.controller.dto.MyCourseListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface CourseSpecification {

    @Operation(summary = "내 코스 목록 조회",
            description = "내 코스를 최신순(id 역순)으로 반환합니다. status 로 여행 상태를 필터링할 수 있으며, "
                    + "생략하면 전체를 반환합니다. 디깅 작성 시 코스를 먼저 고르는 단계에서 사용합니다. "
                    + "코스에는 이름 필드가 없으므로 displayName 은 대표 스팟명으로 합성되고(예: \"영진횟집 외 9곳\"), "
                    + "thumbnail 은 첫 스팟의 이미지입니다. "
                    + "아직 확정하지 않은 draft가 있으면 status=UPCOMING(생략 시에도 포함)일 때 함께 나오며, "
                    + "isConfirmed=false 로 구분할 수 있습니다. sigunguCode 는 코스 자체에 저장된 값이 아니라 "
                    + "코스 첫 스팟의 지역(TouristSpot.ldongSignguCode)에서 가져옵니다.")
    ResponseEntity<MyCourseListResponse> getMyCourses(Long userId, CourseStatus status);

    @Operation(summary = "코스 상세(일자별 스팟) 조회",
            description = "코스의 일자별 스팟 목록을 방문 순서대로 반환합니다. 각 스팟의 gpsVerified 는 해당 코스·일자에서 "
                    + "이미 GPS 인증(POST /api/v1/gps)을 마쳤는지를 나타냅니다. 현장 GPS 인증 화면에서 사용합니다. "
                    + "carRoutes/transitRoutes 는 POST /api/v1/courses/confirm 확정 시 계산해 저장한 구간별 경로로, "
                    + "transportMode 가 CAR면 carRoutes만, PUBLIC_TRANSPORT면 transitRoutes만 값이 채워지고 "
                    + "아직 확정 전(draft)이면 둘 다 null입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코스 상세 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음(본인 소유가 아닌 경우 포함)")
    })
    ResponseEntity<CourseDetailResponse> getCourseDetail(
            @Parameter(hidden = true) @LoginUser Long userId,
            @Parameter(description = "조회할 코스 ID") Long courseId);

    @Operation(summary = "코스 삭제",
            description = "draft/확정/진행중/완료 상태와 무관하게 코스를 삭제합니다. "
                    + "GPS 인증 기록, 디깅(후기) 기록은 삭제하지 않고 그대로 둡니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "코스 삭제 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음(본인 소유가 아닌 경우 포함)")
    })
    ResponseEntity<Void> deleteCourse(
            @Parameter(hidden = true) @LoginUser Long userId,
            @Parameter(description = "삭제할 코스 ID") Long courseId);

    @Operation(
            summary = "AI 코스 추천 생성",
            description = "사용자의 입력 조건을 바탕으로 맞춤형 여행 코스를 추천합니다. "
                    + "여행 기간, 선호 카테고리, 특정 여행지 포함 여부 등을 반영하여 최적의 코스를 구성하며, "
                    + "로그인한 사용자의 고유 식별자(userId)를 기반으로 코스를 생성해 draft로 저장합니다. "
                    + "응답은 courseId만 반환하며, 코스 내용은 GET /api/v1/courses/{courseId}로 조회합니다. "
                    + "사용자당 하루(KST 자정 기준) 생성 가능 횟수는 5회로 제한됩니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코스 추천 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 유효성 검증 실패 (예: 필수값 누락, 일자 범위 오류, 이미 확정된 다른 여행 기간과 겹침 등)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "429", description = "하루 추천 생성 횟수(5회) 초과")
    })
    public ResponseEntity<CourseRecommendResponse> recommend(
            @Parameter(hidden = true) @LoginUser Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "코스 추천에 필요한 요청 정보 (여행 일정, 선호 태그, 출발지 등)",
                    required = true
            )
            @RequestBody @Valid CourseRecommendRequest request
    );

    @Operation(
            summary = "AI 코스 추천 일일 잔여 횟수 조회",
            description = "사용자당 하루(KST 자정 기준) 코스 추천 생성 가능 횟수 중 남은 횟수를 조회합니다. "
                    + "추천 생성 버튼을 누르기 전에 프론트에서 남은 횟수를 미리 보여주거나 버튼을 비활성화하는 용도로 사용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "잔여 횟수 조회 성공"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    })
    ResponseEntity<CourseRecommendQuotaResponse> getRecommendationQuota(
            @Parameter(hidden = true) @LoginUser Long userId);

    @Operation(summary = "draft 임시저장 UPDATE",
            description = "AI 추천 결과(draft)를 확정하기 전, 사용자가 스팟 순서를 바꾸거나 뺀 최종 상태를 draft에 반영합니다. "
                    + "title을 함께 보내면 코스 제목도 함께 수정되며, 생략하거나 빈 값이면 기존 제목이 유지됩니다. "
                    + "이미 확정된 코스는 수정할 수 없습니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "draft 수정 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 유효성 검증 실패 또는 이미 확정된 코스"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "코스 또는 관광지를 찾을 수 없음")
    })
    ResponseEntity<Void> updateDraftDays(
            @Parameter(hidden = true) @LoginUser Long userId,
            @Parameter(description = "수정할 draft 코스 ID") Long courseId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "수정할 코스 제목(선택, 최대 10자)과 일자별 최종 관광지 순서",
                    required = true
            )
            @RequestBody @Valid CourseDayUpdateRequest request);

    @Operation(summary = "코스 확정",
            description = "courseId만으로 draft를 확정합니다. draft 생성 시 고정된 transportMode(CAR/PUBLIC_TRANSPORT)에 따라 "
                    + "서버가 자동으로 자차 또는 대중교통 경로를 계산해 코스에 저장합니다. "
                    + "응답 바디는 없으며, 확정된 코스 내용과 경로는 GET /api/v1/courses/{courseId}로 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "코스 확정 및 경로 계산 성공"),
            @ApiResponse(responseCode = "400", description = "이동수단이 선택되지 않은 코스"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "코스를 찾을 수 없음")
    })
    ResponseEntity<Void> confirm(
            @Parameter(hidden = true) @LoginUser Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "확정할 코스 ID",
                    required = true
            )
            @RequestBody @Valid CourseConfirmRequest request);
}
