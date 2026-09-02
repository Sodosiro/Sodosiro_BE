package com.sodosiro.domain.badge.controller.specification;

import com.sodosiro.domain.badge.controller.dto.BadgeListResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;

public interface BadgeSpecification {

    @Operation(summary = "소도시 뱃지 목록 조회", description = "강원 소도시 지역 뱃지 전체 목록과 로그인한 유저의 획득 여부·획득 시각, 총 획득 개수를 반환합니다.")
    ResponseEntity<BadgeListResponse> getBadges(Long userId);
}
