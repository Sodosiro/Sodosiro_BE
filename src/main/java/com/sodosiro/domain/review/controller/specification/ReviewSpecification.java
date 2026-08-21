package com.sodosiro.domain.review.controller.specification;

import com.sodosiro.domain.review.constants.ReviewSort;
import com.sodosiro.domain.review.controller.dto.request.ReviewCreateRequest;
import com.sodosiro.domain.review.controller.dto.request.ReviewUpdateRequest;
import com.sodosiro.domain.review.controller.dto.response.MyReviewListResponse;
import com.sodosiro.domain.review.controller.dto.response.ReviewListResponse;
import com.sodosiro.domain.review.controller.dto.response.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Tag(name = "리뷰", description = "관광지 리뷰 작성·조회·수정·삭제 API")
public interface ReviewSpecification {

    @Operation(summary = "리뷰 작성",
            description = "관광지에 별점(0.1~5.0, 소수점 한 자리)과 본문으로 리뷰를 작성합니다. 이미지는 선택이며 최대 5장까지 첨부할 수 있습니다. (multipart/form-data)\n\n"
                    + "작성자가 코스 진행 중 이 관광지를 GPS 방문 인증(POST /api/v1/gps, 반경 300m)한 적이 있으면 "
                    + "작성 즉시 방문자 인증(GPS_VERIFIED) 리뷰로 저장됩니다. 별도로 위치를 다시 보낼 필요는 없습니다.")
    ResponseEntity<ReviewResponse> createReview(Long userId, ReviewCreateRequest request, List<MultipartFile> images);

    @Operation(summary = "관광지 리뷰 목록 조회", description = "관광지의 리뷰 목록을 커서 기반으로 조회합니다. hasImage=true 시 이미지가 첨부된 리뷰만 반환합니다.")
    ResponseEntity<ReviewListResponse> getReviews(Long contentId, Long cursor, int size, ReviewSort sort,
            @Parameter(description = "true 시 이미지가 있는 리뷰만 조회") boolean hasImage, Long loginUserId);

    @Operation(summary = "내가 쓴 리뷰 목록", description = "로그인 유저가 작성한 리뷰 목록을 커서 기반으로 조회합니다. hasImage=true 시 이미지가 첨부된 리뷰만 반환합니다.")
    ResponseEntity<MyReviewListResponse> getMyReviews(Long userId, Long cursor, int size,
            @Parameter(description = "정렬 기준 (RECENT, HIGH_RATING, LOW_RATING)") ReviewSort sort,
            @Parameter(description = "true 시 이미지가 있는 리뷰만 조회") boolean hasImage);

    @Operation(summary = "내 리뷰 상세 조회", description = "본인이 작성한 리뷰 한 건을 상세 조회합니다. 타 사용자의 리뷰 조회 시 403을 반환합니다.")
    ResponseEntity<ReviewResponse> getMyReview(Long userId, Long reviewId);

    @Operation(summary = "리뷰 수정", description = "본인이 작성한 리뷰의 별점(0.1~5.0, 소수점 한 자리)·본문·이미지를 수정합니다. (multipart/form-data)")
    ResponseEntity<ReviewResponse> updateReview(Long userId, Long reviewId, ReviewUpdateRequest request, List<MultipartFile> images);

    @Operation(summary = "리뷰 삭제", description = "본인이 작성한 리뷰를 삭제합니다.")
    ResponseEntity<Void> deleteReview(Long userId, Long reviewId);
}
