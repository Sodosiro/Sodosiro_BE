package com.sodosiro.domain.course.controller;

import com.sodosiro.domain.course.controller.dto.CourseConfirmCarRequest;
import com.sodosiro.domain.course.controller.dto.CourseConfirmCarResponse;
import com.sodosiro.domain.course.controller.dto.CourseConfirmPublicTransportRequest;
import com.sodosiro.domain.course.controller.dto.CourseConfirmPublicTransportResponse;
import com.sodosiro.domain.course.controller.dto.CourseRecommendRequest;
import com.sodosiro.domain.course.controller.dto.CourseRecommendResponse;
import com.sodosiro.domain.course.service.CourseConfirmationService;
import com.sodosiro.domain.course.service.CourseRecommendationService;
import com.sodosiro.global.resolver.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/courses")
public class CourseController {

    private final CourseRecommendationService courseRecommendationService;
    private final CourseConfirmationService courseConfirmationService;

    @PostMapping("/recommendations")
    public ResponseEntity<CourseRecommendResponse> recommend(
            @LoginUser Long userId, @RequestBody @Valid CourseRecommendRequest request) {
        return ResponseEntity.ok(courseRecommendationService.recommend(userId, request));
    }

    @PostMapping("/confirm/car")
    public ResponseEntity<CourseConfirmCarResponse> confirmCar(
            @LoginUser Long userId, @RequestBody @Valid CourseConfirmCarRequest request) {
        return ResponseEntity.ok(courseConfirmationService.confirmCar(userId, request));
    }

    @PostMapping("/confirm/public-transport")
    public ResponseEntity<CourseConfirmPublicTransportResponse> confirmPublicTransport(
            @LoginUser Long userId, @RequestBody @Valid CourseConfirmPublicTransportRequest request) {
        return ResponseEntity.ok(courseConfirmationService.confirmPublicTransport(userId, request));
    }
}