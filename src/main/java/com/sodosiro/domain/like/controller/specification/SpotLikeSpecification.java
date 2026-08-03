package com.sodosiro.domain.like.controller.specification;

import com.sodosiro.domain.like.controller.dto.response.LikeToggleResponse;
import com.sodosiro.domain.like.controller.dto.response.MyLikedSpotListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;

@Tag(name = "좋아요", description = "관광지 좋아요 토글 및 내 목록 조회")
public interface SpotLikeSpecification {

    @Operation(summary = "관광지 좋아요 토글",
               description = "좋아요가 없으면 추가, 있으면 취소합니다. 응답의 liked 필드로 현재 상태를 확인하세요.")
    ResponseEntity<LikeToggleResponse> toggleTouristSpotLike(Long userId, Long contentId);

    @Operation(summary = "내가 좋아요한 관광지 목록",
               description = "커서 기반 페이지네이션. 첫 요청은 cursor 생략, 이후 응답의 nextCursor 값을 사용하세요.")
    ResponseEntity<MyLikedSpotListResponse> getMyLikedSpots(
            Long userId,
            Long cursor,
            @Min(1) @Max(100) int size);
}
