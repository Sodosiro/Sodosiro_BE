package com.sodosiro.domain.notification.location.controller.specification;

import com.sodosiro.domain.notification.location.controller.dto.LocationUpdateApiRequest;
import com.sodosiro.domain.notification.location.controller.dto.LocationUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

@Tag(name = "위치", description = "실시간 좌표를 전달해 근처 찜 장소 알림을 트리거하는 API")
public interface LocationSpecification {

    @Operation(
            summary = "위치 업데이트 및 근처 알림 트리거",
            description = "JWT로 식별한 사용자의 확정·진행중(IN_PROGRESS) 코스를 기준으로 반경 200m 이내 좋아요 장소를 조회합니다. "
                    + "courseId는 별도로 받지 않으며, 코스 확정(confirm) 시점에 Redis에 캐싱된 사용자별 활성 코스 스냅샷을 사용합니다. "
                    + "새로 진입한 장소가 있으면 알림 발송 조건(동일 장소 여행 중 1회, 사용자 전체 최소 4시간 간격, KST 하루 최대 2회)을 만족할 때만 알림을 생성합니다. "
                    + "확정된 코스가 없거나 오늘이 여행 기간 밖이면 processed=false, ignoredReason=COURSE_NOT_IN_PROGRESS로 응답합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "처리 성공 (활성 코스가 없어 무시된 경우 포함)"),
            @ApiResponse(responseCode = "400", description = "좌표/정확도 형식이 유효하지 않거나(accuracy > 100m), 위치 이벤트가 5분보다 오래됨")
    })
    ResponseEntity<LocationUpdateResponse> updateLocation(
            Long userId,
            @Valid LocationUpdateApiRequest request
    );
}
