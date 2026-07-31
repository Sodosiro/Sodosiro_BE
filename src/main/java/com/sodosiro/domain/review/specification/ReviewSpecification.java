package com.sodosiro.domain.review.specification;

import com.sodosiro.domain.review.constants.ReviewSort;
import com.sodosiro.domain.review.controller.dto.request.ReviewCreateRequest;
import com.sodosiro.domain.review.controller.dto.request.ReviewUpdateRequest;
import com.sodosiro.domain.review.controller.dto.response.MyReviewListResponse;
import com.sodosiro.domain.review.controller.dto.response.ReviewListResponse;
import com.sodosiro.domain.review.controller.dto.response.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ReviewSpecification {

    /**
     * 관광지에 별점, 본문 및 이미지를 포함한 리뷰를 작성합니다.
     *
     * @param userId  리뷰를 작성하는 사용자의 식별자
     * @param request 리뷰의 별점과 본문
     * @param images  리뷰에 첨부할 이미지 목록
     * @return        작성된 리뷰 정보
     */
    @Operation(summary = "리뷰 작성", description = "관광지에 별점(1~5), 이미지(최대 5장), 본문으로 리뷰를 작성합니다. (multipart/form-data)")
    ResponseEntity<ReviewResponse> createReview(Long userId, ReviewCreateRequest request, List<MultipartFile> images);

    /**
     * 관광지의 리뷰 목록을 조회합니다.
     *
     * @param contentId 관광지 식별자
     * @param cursor    다음 조회를 시작할 리뷰 커서
     * @param size      조회할 리뷰 수
     * @param sort      리뷰 정렬 기준
     * @param loginUserId 로그인한 사용자 식별자
     * @return 관광지 리뷰 목록 응답
     */
    @Operation(summary = "관광지 리뷰 목록 조회", description = "관광지의 리뷰 목록을 커서 기반으로 조회합니다.")
    ResponseEntity<ReviewListResponse> getReviews(Long contentId, Long cursor, int size, ReviewSort sort, Long loginUserId);

    /**
     * Retrieves reviews written by the authenticated user using cursor-based pagination.
     *
     * @param userId the authenticated user's identifier
     * @param cursor the pagination cursor
     * @param size the maximum number of reviews to retrieve
     * @return the user's paginated review list
     */
    @Operation(summary = "내가 쓴 리뷰 목록", description = "로그인 유저가 작성한 리뷰 목록을 커서 기반으로 조회합니다.")
    ResponseEntity<MyReviewListResponse> getMyReviews(Long userId, Long cursor, int size);

    /**
     * Updates a review authored by the specified user.
     *
     * @param userId  the review author's identifier
     * @param reviewId the identifier of the review to update
     * @param request the updated rating and review text
     * @param images  the updated review images
     * @return the updated review
     */
    @Operation(summary = "리뷰 수정", description = "본인이 작성한 리뷰의 별점·본문·이미지를 수정합니다. (multipart/form-data)")
    ResponseEntity<ReviewResponse> updateReview(Long userId, Long reviewId, ReviewUpdateRequest request, List<MultipartFile> images);

    /**
     * 본인이 작성한 리뷰를 삭제합니다.
     *
     * @param userId   리뷰 작성자의 사용자 식별자
     * @param reviewId 삭제할 리뷰 식별자
     */
    @Operation(summary = "리뷰 삭제", description = "본인이 작성한 리뷰를 삭제합니다.")
    ResponseEntity<Void> deleteReview(Long userId, Long reviewId);
}
