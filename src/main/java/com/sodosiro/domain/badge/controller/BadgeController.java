package com.sodosiro.domain.badge.controller;

import com.sodosiro.domain.badge.controller.dto.BadgeListResponse;
import com.sodosiro.domain.badge.controller.specification.BadgeSpecification;
import com.sodosiro.domain.badge.service.BadgeService;
import com.sodosiro.global.resolver.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/badges")
public class BadgeController implements BadgeSpecification {

    private final BadgeService badgeService;

    @GetMapping
    @Override
    public ResponseEntity<BadgeListResponse> getBadges(@LoginUser Long userId) {
        return ResponseEntity.ok(badgeService.getBadges(userId));
    }
}
