package com.sodosiro.domain.gps.controller.specification;

import com.sodosiro.domain.gps.controller.dto.request.GpsRequest;
import com.sodosiro.domain.gps.controller.dto.response.GpsResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;

public interface GpsSpecification {

    @Operation(summary = "코스 스팟 GPS 방문 인증", description = "본인 코스의 특정 일자·관광지에 대해 현재 GPS 좌표와 관광지 좌표의 거리가 300m 이내이면 인증 레코드를 새로 생성합니다. "
            + "300m 밖이면 레코드를 만들지 않고 오류를 반환하며, 이미 인증된 스팟이면 기존 인증 결과를 그대로 반환합니다. 원본 GPS 좌표는 저장하지 않습니다.")
    ResponseEntity<GpsResponse> verify(Long userId, GpsRequest request);
}
