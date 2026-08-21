package com.sodosiro.domain.notification.controller;

import com.sodosiro.domain.dataextract.service.InternalEtlTokenVerifier;
import com.sodosiro.domain.notification.controller.dto.ReviewRequestBatchRequest;
import com.sodosiro.domain.notification.controller.dto.ReviewRequestBatchResponse;
import com.sodosiro.domain.notification.service.ReviewRequestBatchService;
import com.sodosiro.domain.notification.service.dto.ReviewRequestBatchResult;
import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ETL(Airflow) 전용 알림 배치 트리거.
 */
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/etl/notifications")
public class ReviewRequestEtlController {

    private final ReviewRequestBatchService reviewRequestBatchService;
    private final InternalEtlTokenVerifier internalEtlTokenVerifier;

    @PostMapping("/review-requests")
    public ResponseEntity<ReviewRequestBatchResponse> sendReviewRequests(
            @RequestHeader(value = "X-Internal-ETL-Token", required = false) String internalEtlToken,   // ETL 서버 내부에 있음
            @RequestBody(required = false) ReviewRequestBatchRequest request) {
        internalEtlTokenVerifier.verify(internalEtlToken);
        ReviewRequestBatchResult result = reviewRequestBatchService.run(request == null ? null : request.runId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ReviewRequestBatchResponse.from(result));
    }
}
