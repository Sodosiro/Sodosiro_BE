package com.sodosiro.domain.notification.controller;

import com.sodosiro.domain.dataextract.service.InternalEtlTokenVerifier;
import com.sodosiro.domain.notification.controller.dto.CourseConfirmReminderBatchRequest;
import com.sodosiro.domain.notification.controller.dto.CourseConfirmReminderBatchResponse;
import com.sodosiro.domain.notification.service.CourseConfirmReminderBatchService;
import com.sodosiro.domain.notification.service.dto.CourseConfirmReminderBatchResult;
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
 * ETL(Airflow) 전용 코스 확정 유도 알림 배치 트리거.
 */
@Hidden
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/etl/notifications")
public class CourseConfirmReminderEtlController {

    private final CourseConfirmReminderBatchService courseConfirmReminderBatchService;
    private final InternalEtlTokenVerifier internalEtlTokenVerifier;

    @PostMapping("/course-confirm-reminders")
    public ResponseEntity<CourseConfirmReminderBatchResponse> sendCourseConfirmReminders(
            @RequestHeader(value = "X-Internal-ETL-Token", required = false) String internalEtlToken,
            @RequestBody(required = false) CourseConfirmReminderBatchRequest request) {
        internalEtlTokenVerifier.verify(internalEtlToken);
        CourseConfirmReminderBatchResult result =
                courseConfirmReminderBatchService.run(request == null ? null : request.runId());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(CourseConfirmReminderBatchResponse.from(result));
    }
}
