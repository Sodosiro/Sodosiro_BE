package com.sodosiro.domain.gps.controller.specification;

import com.sodosiro.domain.gps.controller.dto.request.GpsRequest;
import com.sodosiro.domain.gps.controller.dto.response.GpsResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;

public interface GpsSpecification {

    @Operation(summary = "코스 스팟 GPS 방문 인증", description = "위치 인증은 프론트에서 완료한 뒤 호출합니다. 본인 코스의 특정 일자·관광지에 대해 인증 레코드를 새로 생성하며, "
            + "이미 인증된 스팟이면 기존 인증 결과를 그대로 반환합니다.")
    ResponseEntity<GpsResponse> verify(Long userId, GpsRequest request);
}
