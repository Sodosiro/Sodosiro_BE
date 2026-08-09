package com.sodosiro.domain.user.controller;

import com.sodosiro.domain.user.dto.request.ProfileRequest;
import com.sodosiro.domain.user.dto.response.ProfileResponse;
import com.sodosiro.domain.user.service.UserService;
import com.sodosiro.domain.user.specification.UserSpecification;
import com.sodosiro.global.resolver.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/me")
@RequiredArgsConstructor
public class Usercontroller implements UserSpecification {

    private final UserService userService;

    @GetMapping
    public ResponseEntity<ProfileResponse> getProfile(@LoginUser Long userId) {
        ProfileResponse response = userService.getProfile(userId);

        return ResponseEntity.ok(response);
    }

    @PatchMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProfileResponse> updateProfile(
            @LoginUser Long userId,
            @RequestPart @Valid ProfileRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image) {

        ProfileResponse response = userService.updateProfile(userId, request,image);

        return ResponseEntity.ok(response);
    }
}
