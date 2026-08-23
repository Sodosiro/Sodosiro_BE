package com.sodosiro.domain.course.controller.specification;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.controller.dto.CourseConfirmCarRequest;
import com.sodosiro.domain.course.controller.dto.CourseConfirmCarResponse;
import com.sodosiro.domain.course.controller.dto.CourseConfirmPublicTransportRequest;
import com.sodosiro.domain.course.controller.dto.CourseConfirmPublicTransportResponse;
import com.sodosiro.domain.course.controller.dto.CourseConfirmRequest;
import com.sodosiro.domain.course.controller.dto.CourseDayUpdateRequest;
import com.sodosiro.domain.course.controller.dto.CourseDetailResponse;
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
            description = "확정된 내 코스를 최신순으로 반환합니다. status 로 여행 상태를 필터링할 수 있으며, "
                    + "생략하면 확정된 코스를 모두 반환합니다. 디깅 작성 시 코스를 먼저 고르는 단계에서 사용합니다. "
                    + "코스에는 이름 필드가 없으므로 displayName 은 대표 스팟명으로 합성되고(예: \"영진횟집 외 9곳\"), "
                    + "thumbnail 은 첫 스팟의 이미지입니다.")
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

    @Operation(
            summary = "AI 코스 추천 생성",
            description = "사용자의 입력 조건을 바탕으로 맞춤형 여행 코스를 추천합니다. "
                    + "여행 기간, 선호 카테고리, 특정 여행지 포함 여부 등을 반영하여 최적의 코스를 구성하며, "
                    + "로그인한 사용자의 고유 식별자(userId)를 기반으로 코스를 생성해 draft로 저장합니다. "
                    + "응답은 courseId만 반환하며, 코스 내용은 GET /api/v1/courses/{courseId}로 조회합니다."
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

    @Operation(
            summary = "코스 확정 (자차)",
            description = "프론트에서 최종 확정한 일자별 관광지 순서를 받아 코스를 확정 상태(is_confirmed=true)로 전환하고, "
                    + "카카오 자동차 길찾기 API로 계산한 구간별 소요시간/거리와 지도 표시용 경로 좌표(코너 좌표)를 함께 반환합니다.\n\n"
                    + "[응답 필드 설명 - days[].legs[]]\n"
                    + "- day: 여행 일자 (1일차, 2일차 ...)\n"
                    + "- fromId / toId: 출발/도착 관광지 ID\n"
                    + "- durationSeconds: 예상 소요 시간(초)\n"
                    + "- distanceMeters: 이동 거리(m)\n"
                    + "- tollFare: 통행료(원)\n"
                    + "- estimatedFuelCost: 예상 유류비(원)\n"
                    + "- path: 지도에 경로선을 그리기 위한 좌표 목록 (longitude, latitude)\n"
                    + "- success: 경로 계산 성공 여부 (false면 durationSeconds 등 나머지 값은 모두 null)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코스 확정 및 자차 경로 계산 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 유효성 검증 실패 (예: 일자별 관광지 목록 누락)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "코스 또는 관광지를 찾을 수 없음")
    })
    public ResponseEntity<CourseConfirmCarResponse> confirmCar(
            @Parameter(hidden = true) @LoginUser Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "확정할 코스 ID와 일자별 최종 관광지 순서",
                    required = true
            )
            @RequestBody @Valid CourseConfirmCarRequest request
    );

    @Operation(
            summary = "코스 확정 (대중교통)",
            description = "프론트에서 최종 확정한 일자별 관광지 순서를 받아 코스를 확정 상태(is_confirmed=true)로 전환하고, "
                    + "카카오 대중교통 길찾기 API로 계산한 구간별 소요시간/거리/환승/요금과 지도 표시용 경로 좌표(단계별 좌표 포함)를 함께 반환합니다.\n\n"
                    + "[응답 필드 설명 - days[].details[]]\n"
                    + "- day: 여행 일자 (1일차, 2일차 ...)\n"
                    + "- success: 경로 탐색 성공 여부 (false면 나머지 값은 모두 null)\n"
                    + "- type: 경로 타입 (카카오 API 반환값)\n"
                    + "- totalTimeSeconds: 총 소요 시간(초)\n"
                    + "- totalDistanceMeters: 총 이동 거리(m)\n"
                    + "- transfers: 환승 횟수\n"
                    + "- fare: 요금(원)\n"
                    + "- steps[]: 도보/버스/지하철 등 구간별 상세\n"
                    + "  - type: 단계 종류 (도보/버스/지하철 등)\n"
                    + "  - guidance: 안내 문구\n"
                    + "  - distanceMeters / timeSeconds: 해당 단계 거리(m) / 소요 시간(초)\n"
                    + "  - stopNames: 정류장/역 이름 목록\n"
                    + "  - vehicleNames: 버스 번호/지하철 호선 등 노선 이름 목록\n"
                    + "  - path: 지도에 단계별로 색을 구분해 그릴 좌표 목록 (longitude, latitude)"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "코스 확정 및 대중교통 경로 계산 성공"),
            @ApiResponse(responseCode = "400", description = "요청 파라미터 유효성 검증 실패 (예: 일자별 관광지 목록 누락)"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자"),
            @ApiResponse(responseCode = "404", description = "코스 또는 관광지를 찾을 수 없음")
    })
    public ResponseEntity<CourseConfirmPublicTransportResponse> confirmPublicTransport(
            @Parameter(hidden = true) @LoginUser Long userId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "확정할 코스 ID와 일자별 최종 관광지 순서",
                    required = true
            )
            @RequestBody @Valid CourseConfirmPublicTransportRequest request
    );

    @Operation(summary = "확정 전 draft 일자별 관광지 순서 수정",
            description = "AI 추천 결과(draft)를 확정하기 전, 사용자가 스팟 순서를 바꾸거나 뺀 최종 상태를 draft에 반영합니다. "
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
                    description = "일자별 최종 관광지 순서",
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
