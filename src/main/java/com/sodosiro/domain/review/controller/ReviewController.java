package com.sodosiro.domain.review.controller;

import com.sodosiro.domain.review.constants.ReviewSort;
import com.sodosiro.domain.review.controller.dto.request.ReviewCreateRequest;
import com.sodosiro.domain.review.controller.dto.request.ReviewUpdateRequest;
import com.sodosiro.domain.review.controller.dto.response.MyReviewListResponse;
import com.sodosiro.domain.review.controller.dto.response.ReviewListResponse;
import com.sodosiro.domain.review.controller.dto.response.ReviewResponse;
import com.sodosiro.domain.review.service.ReviewService;
import com.sodosiro.domain.review.controller.specification.ReviewSpecification;
import com.sodosiro.global.resolver.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController implements ReviewSpecification {

    private final ReviewService reviewService;

    @PostMapping(value = "/reviews", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponse> createReview(
            @LoginUser Long userId,
            @RequestPart @Valid ReviewCreateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        ReviewResponse response = reviewService.createReview(userId, request, images);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/spots/{contentId}/reviews")
    public ResponseEntity<ReviewListResponse> getReviews(
            @PathVariable Long contentId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "RECENT") ReviewSort sort,
            @RequestParam(defaultValue = "false") boolean hasImage,
            @LoginUser Long loginUserId) {

        ReviewListResponse response = reviewService.getReviews(contentId, cursor, size, sort, hasImage, loginUserId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reviews/me")
    public ResponseEntity<MyReviewListResponse> getMyReviews(
            @LoginUser Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "RECENT") ReviewSort sort,
            @RequestParam(defaultValue = "false") boolean hasImage) {

        MyReviewListResponse response = reviewService.getMyReviews(userId, cursor, size, sort, hasImage);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reviews/{reviewId}")
    public ResponseEntity<ReviewResponse> getMyReview(
            @LoginUser Long userId,
            @PathVariable Long reviewId) {

        return ResponseEntity.ok(reviewService.getMyReview(userId, reviewId));
    }

    @PatchMapping(value = "/reviews/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ReviewResponse> updateReview(
            @LoginUser Long userId,
            @PathVariable Long reviewId,
            @RequestPart @Valid ReviewUpdateRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images) {

        ReviewResponse response = reviewService.updateReview(userId, reviewId, request, images);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @LoginUser Long userId,
            @PathVariable Long reviewId) {

        reviewService.deleteReview(userId, reviewId);
        return ResponseEntity.noContent().build();
    }
}
