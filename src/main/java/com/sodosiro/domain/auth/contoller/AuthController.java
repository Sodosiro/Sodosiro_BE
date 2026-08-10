package com.sodosiro.domain.auth.contoller;

import com.sodosiro.domain.auth.contoller.dto.request.LogoutRequest;
import com.sodosiro.domain.auth.contoller.dto.request.ReissueRequest;
import com.sodosiro.domain.auth.contoller.dto.request.SocialLoginRequest;
import com.sodosiro.domain.auth.contoller.dto.response.ReissueResponse;
import com.sodosiro.domain.auth.contoller.dto.response.SocialLoginResponse;
import com.sodosiro.domain.auth.service.AuthService;
import com.sodosiro.domain.auth.contoller.specification.AuthSpecification;
import com.sodosiro.global.resolver.LoginUser;
import com.sodosiro.global.utils.AuthorizationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/reissue")
    public ResponseEntity<ReissueResponse> reissue(@RequestBody ReissueRequest refreshToken) {

        ReissueResponse response = authService.reissueToken(refreshToken.refreshToken());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/app/logout")
    public ResponseEntity<Void> appLogout(@LoginUser Long userId,
                                          @RequestBody LogoutRequest token,
                                          @RequestHeader("Authorization") String authorization) {

        String accessToken = AuthorizationUtils.extractBearerToken(authorization);
        authService.logout(userId, accessToken, token.refreshToken());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@LoginUser Long userId,
                                         @RequestBody LogoutRequest token,
                                         @RequestHeader("Authorization") String authorization) {

        String accessToken = AuthorizationUtils.extractBearerToken(authorization);
        authService.withdraw(userId, accessToken, token.refreshToken());
        return ResponseEntity.noContent().build();
    }
}
