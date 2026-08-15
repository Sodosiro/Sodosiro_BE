package com.sodosiro.domain.like.controller;

import com.sodosiro.domain.like.controller.dto.request.SpotLikeToggleRequest;
import com.sodosiro.domain.like.controller.dto.response.LikeToggleResponse;
import com.sodosiro.domain.like.controller.dto.response.MyLikedSpotListResponse;
import com.sodosiro.domain.like.controller.dto.response.SpotLikeBatchToggleResponse;
import com.sodosiro.domain.like.controller.specification.SpotLikeSpecification;
import com.sodosiro.domain.like.service.SpotLikeService;
import com.sodosiro.global.resolver.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SpotLikeController implements SpotLikeSpecification {

    private final SpotLikeService spotLikeService;

    @PostMapping("/spots/{contentId}/like")
    public ResponseEntity<LikeToggleResponse> toggleTouristSpotLike(
            @LoginUser Long userId,
            @PathVariable Long contentId) {

        return ResponseEntity.ok(spotLikeService.toggleTouristSpotLike(userId, contentId));
    }

    @PostMapping("/spots/likes/toggle")
    public ResponseEntity<SpotLikeBatchToggleResponse> toggleTouristSpotLikes(
            @LoginUser Long userId,
            @RequestBody @Valid SpotLikeToggleRequest request) {
        return ResponseEntity.ok(spotLikeService.toggleTouristSpotLikes(userId, request.contentIds()));
    }

    @GetMapping("/likes")
    public ResponseEntity<MyLikedSpotListResponse> getMyLikedSpots(
            @LoginUser Long userId,
            @RequestParam(required = false) String sigunguCode,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "5") int size) {

        return ResponseEntity.ok(spotLikeService.getMyLikedSpots(userId, sigunguCode, cursor, size));
    }
}
