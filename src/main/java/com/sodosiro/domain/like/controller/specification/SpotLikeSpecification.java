package com.sodosiro.domain.like.controller.specification;

import com.sodosiro.domain.like.controller.dto.request.SpotLikeToggleRequest;
import com.sodosiro.domain.like.controller.dto.response.LikeToggleResponse;
import com.sodosiro.domain.like.controller.dto.response.MyLikedSpotListResponse;
import com.sodosiro.domain.like.controller.dto.response.SpotLikeBatchToggleResponse;
import com.sodosiro.domain.review.constants.ReviewSort;
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

    @Operation(summary = "관광지 좋아요 일괄 토글",
            description = "contentIds에 한 개 또는 여러 개의 관광지 ID를 전달합니다. 각 항목은 현재 상태를 기준으로 토글되며, 중복되거나 존재하지 않는 ID가 있으면 전체 요청이 적용되지 않습니다.")
    ResponseEntity<SpotLikeBatchToggleResponse> toggleTouristSpotLikes(
            Long userId, SpotLikeToggleRequest request);

    @Operation(summary = "내가 좋아요한 관광지 목록 (지역별)",
               description = "GET /api/v1/travel-spots/likes. sigunguCode에 해당하는 지역의 좋아요 목록만 조회합니다(NULL 시 전체조회). RECENT, HIGH_RATING, LOW_RATING 정렬과 커서 기반 무한스크롤을 지원합니다. 첫 요청은 cursor 생략, 이후 응답의 nextCursor 값을 사용하세요.")
    ResponseEntity<MyLikedSpotListResponse> getMyLikedSpots(
            Long userId,
            String sigunguCode,
            String cursor,
            @Min(1) @Max(100) int size,
            ReviewSort sort);
}
