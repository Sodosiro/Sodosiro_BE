package com.sodosiro.domain.course.controller.specification;

import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.controller.dto.CourseConfirmCarRequest;
import com.sodosiro.domain.course.controller.dto.CourseConfirmCarResponse;
import com.sodosiro.domain.course.controller.dto.CourseConfirmPublicTransportRequest;
import com.sodosiro.domain.course.controller.dto.CourseConfirmPublicTransportResponse;
import com.sodosiro.domain.course.controller.dto.CourseRecommendRequest;
import com.sodosiro.domain.course.controller.dto.CourseRecommendResponse;
import com.sodosiro.domain.course.controller.dto.MyCourseListResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;

public interface CourseSpecification {

    @Operation(summary = "내 코스 목록 조회",
            description = "확정된 내 코스를 최신순으로 반환합니다. status 로 여행 상태를 필터링할 수 있으며, "
                    + "생략하면 확정된 코스를 모두 반환합니다. 디깅 작성 시 코스를 먼저 고르는 단계에서 사용합니다. "
                    + "코스에는 이름 필드가 없으므로 displayName 은 대표 스팟명으로 합성되고(예: \"영진횟집 외 9곳\"), "
                    + "thumbnail 은 첫 스팟의 이미지입니다.")
    ResponseEntity<MyCourseListResponse> getMyCourses(Long userId, CourseStatus status);

    @Operation(summary = "AI 코스 추천",
            description = "여행 기간·스타일·필수 방문지를 바탕으로 일자별 코스를 추천하고 미확정 draft 로 저장합니다. "
                    + "사용자당 미확정 draft 는 1개만 유지되며, 다시 호출하면 기존 draft 를 대체합니다.")
    ResponseEntity<CourseRecommendResponse> recommend(Long userId, CourseRecommendRequest request);

    @Operation(summary = "코스 확정 (자동차)",
            description = "추천받은 코스를 자동차 이동 기준으로 확정하고, 일자별 구간 경로를 반환합니다.")
    ResponseEntity<CourseConfirmCarResponse> confirmCar(Long userId, CourseConfirmCarRequest request);

    @Operation(summary = "코스 확정 (대중교통)",
            description = "추천받은 코스를 대중교통 이동 기준으로 확정하고, 일자별 구간 경로를 반환합니다.")
    ResponseEntity<CourseConfirmPublicTransportResponse> confirmPublicTransport(
            Long userId, CourseConfirmPublicTransportRequest request);
}
