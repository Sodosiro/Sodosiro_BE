package com.sodosiro.domain.review.specification;

import com.sodosiro.domain.review.constants.ReviewSort;
import com.sodosiro.domain.review.controller.dto.request.ReviewCreateRequest;
import com.sodosiro.domain.review.controller.dto.request.ReviewGpsVerificationRequest;
import com.sodosiro.domain.review.controller.dto.request.ReviewUpdateRequest;
import com.sodosiro.domain.review.controller.dto.response.MyReviewListResponse;
import com.sodosiro.domain.review.controller.dto.response.ReviewListResponse;
import com.sodosiro.domain.review.controller.dto.response.ReviewGpsVerificationResponse;
import com.sodosiro.domain.review.controller.dto.response.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReviewSpecification {

    @Operation(summary = "리뷰 작성", description = "관광지에 별점(1.0~5.0, 소수점 한 자리)과 본문으로 리뷰를 작성합니다. 이미지는 선택이며 최대 5장까지 첨부할 수 있습니다. (multipart/form-data)")
    ResponseEntity<ReviewResponse> createReview(Long userId, ReviewCreateRequest request, List<MultipartFile> images);

    @Operation(summary = "리뷰 GPS 방문 인증", description = "본인 리뷰에 대해 현재 GPS 좌표와 관광지 좌표의 거리가 1km 이내이면 GPS 인증 리뷰로 승격합니다. 원본 GPS 좌표는 저장하지 않습니다.")
    ResponseEntity<ReviewGpsVerificationResponse> verifyGpsVisit(
            Long userId, Long reviewId, ReviewGpsVerificationRequest request);

    @Operation(summary = "관광지 리뷰 목록 조회", description = "관광지의 리뷰 목록을 커서 기반으로 조회합니다.")
    ResponseEntity<ReviewListResponse> getReviews(Long contentId, Long cursor, int size, ReviewSort sort, Long loginUserId);

    @Operation(summary = "내가 쓴 리뷰 목록", description = "로그인 유저가 작성한 리뷰 목록을 커서 기반으로 조회합니다.")
    ResponseEntity<MyReviewListResponse> getMyReviews(Long userId, Long cursor, int size);

    @Operation(summary = "리뷰 수정", description = "본인이 작성한 리뷰의 별점(1.0~5.0, 소수점 한 자리)·본문·이미지를 수정합니다. (multipart/form-data)")
    ResponseEntity<ReviewResponse> updateReview(Long userId, Long reviewId, ReviewUpdateRequest request, List<MultipartFile> images);

    @Operation(summary = "리뷰 삭제", description = "본인이 작성한 리뷰를 삭제합니다.")
    ResponseEntity<Void> deleteReview(Long userId, Long reviewId);
}
