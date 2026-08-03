package com.sodosiro.domain.like.controller;

import com.sodosiro.domain.like.controller.dto.response.LikeToggleResponse;
import com.sodosiro.domain.like.controller.dto.response.MyLikedSpotListResponse;
import com.sodosiro.domain.like.controller.specification.SpotLikeSpecification;
import com.sodosiro.domain.like.service.SpotLikeService;
import com.sodosiro.global.resolver.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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

    @GetMapping("/likes/me")
    public ResponseEntity<MyLikedSpotListResponse> getMyLikedSpots(
            @LoginUser Long userId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(spotLikeService.getMyLikedSpots(userId, cursor, size));
    }
}
