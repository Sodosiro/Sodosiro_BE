package com.sodosiro.domain.auth.contoller;

import com.sodosiro.domain.auth.dto.request.SocialLoginRequest;
import com.sodosiro.domain.auth.dto.response.SocialLoginResponse;
import com.sodosiro.domain.auth.service.AuthService;
import com.sodosiro.domain.auth.specification.AuthSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/auth")
public class AuthController implements AuthSpecification {

    private final AuthService authService;

    @PostMapping("/social")
    public ResponseEntity<SocialLoginResponse> socialLogin(@RequestBody SocialLoginRequest request) {

        SocialLoginResponse response = authService.loginWithSocial(request.provider(), request.idToken(), request.authorizationCode());

        return ResponseEntity.ok(response);
    }
}