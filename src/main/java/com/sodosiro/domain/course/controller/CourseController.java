package com.sodosiro.domain.course.controller;

import com.sodosiro.domain.course.controller.dto.CourseConfirmCarRequest;
import com.sodosiro.domain.course.controller.dto.CourseConfirmCarResponse;
import com.sodosiro.domain.course.controller.dto.CourseConfirmPublicTransportRequest;
import com.sodosiro.domain.course.controller.dto.CourseConfirmPublicTransportResponse;
import com.sodosiro.domain.course.controller.dto.CourseConfirmRequest;
import com.sodosiro.domain.course.controller.dto.CourseDayUpdateRequest;
import com.sodosiro.domain.course.constants.CourseStatus;
import com.sodosiro.domain.course.controller.dto.CourseDetailResponse;
import com.sodosiro.domain.course.controller.dto.CourseRecommendRequest;
import com.sodosiro.domain.course.controller.dto.CourseRecommendResponse;
import com.sodosiro.domain.course.controller.dto.MyCourseListResponse;
import com.sodosiro.domain.course.controller.specification.CourseSpecification;
import com.sodosiro.domain.course.service.CourseConfirmationService;
import com.sodosiro.domain.course.service.CourseQueryService;
import com.sodosiro.domain.course.service.CourseRecommendationService;
import com.sodosiro.global.resolver.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CourseController implements CourseSpecification {

    private final CourseRecommendationService courseRecommendationService;
    private final CourseConfirmationService courseConfirmationService;
    private final CourseQueryService courseQueryService;

    @GetMapping("/me")
    public ResponseEntity<MyCourseListResponse> getMyCourses(
            @LoginUser Long userId,
            @RequestParam(required = false) CourseStatus status) {
        return ResponseEntity.ok(courseQueryService.getMyCourses(userId, status));
    }

    @GetMapping("/{courseId}")
    public ResponseEntity<CourseDetailResponse> getCourseDetail(
            @LoginUser Long userId, @PathVariable Long courseId) {
        return ResponseEntity.ok(courseQueryService.getCourseDetail(userId, courseId));
    }

    @DeleteMapping("/{courseId}")
    public ResponseEntity<Void> deleteCourse(@LoginUser Long userId, @PathVariable Long courseId) {
        courseQueryService.deleteCourse(userId, courseId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/recommendations")
    public ResponseEntity<CourseRecommendResponse> recommend(@LoginUser Long userId, @RequestBody @Valid CourseRecommendRequest request) {

        return ResponseEntity.ok(courseRecommendationService.recommend(userId, request));
    }

    @PostMapping("/confirm/car")
    public ResponseEntity<CourseConfirmCarResponse> confirmCar(@LoginUser Long userId, @RequestBody @Valid CourseConfirmCarRequest request) {

        return ResponseEntity.ok(courseConfirmationService.confirmCar(userId, request));
    }

    @PostMapping("/confirm/public-transport")
    public ResponseEntity<CourseConfirmPublicTransportResponse> confirmPublicTransport(@LoginUser Long userId, @RequestBody @Valid CourseConfirmPublicTransportRequest request) {

        return ResponseEntity.ok(courseConfirmationService.confirmPublicTransport(userId, request));
    }

    @PatchMapping("/{courseId}/days")
    public ResponseEntity<Void> updateDraftDays(@LoginUser Long userId,
                                                @PathVariable Long courseId,
                                                @RequestBody @Valid CourseDayUpdateRequest request) {

        courseConfirmationService.updateDraftDays(userId, courseId, request.days());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/confirm")
    public ResponseEntity<Void> confirm(@LoginUser Long userId, @RequestBody @Valid CourseConfirmRequest request) {
        courseConfirmationService.confirm(userId, request);
        return ResponseEntity.noContent().build();
    }
}