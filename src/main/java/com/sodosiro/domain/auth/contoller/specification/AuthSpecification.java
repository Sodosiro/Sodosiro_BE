package com.sodosiro.domain.auth.contoller.specification;

import com.sodosiro.domain.auth.contoller.dto.request.LogoutRequest;
import com.sodosiro.domain.auth.contoller.dto.request.ReissueRequest;
import com.sodosiro.domain.auth.contoller.dto.request.SocialLoginRequest;
import com.sodosiro.domain.auth.contoller.dto.response.ReissueResponse;
import com.sodosiro.domain.auth.contoller.dto.response.SocialLoginResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

public interface AuthSpecification {

    @PermitAll
    @Operation(summary = "소셜 SDK 토큰 검증 후 jwt 발급",
            description = "토큰 만료일 : refresh: 7일 , access: 15분")
    public ResponseEntity<SocialLoginResponse> socialLogin(@RequestBody SocialLoginRequest request);

    @PermitAll
    @Operation(summary = "토큰 재발급",
            description = "Refresh Token을 이용해 새로운 Access Token를 발급합니다.")
    ResponseEntity<ReissueResponse> reissue(@RequestBody ReissueRequest refreshToken);

    @Operation(summary = "앱 로그아웃",
            description = "Access Token과 Refresh Token을 무효화하고 로그아웃합니다.")
    ResponseEntity<Void> appLogout(Long userId,
                                   @RequestBody LogoutRequest token,
                                   @RequestHeader("Authorization") String authorization);

    @Operation(summary = "회원 탈퇴",
            description = "회원 탈퇴를 진행하고 토큰 및 유저 정보를 삭제합니다.")
    ResponseEntity<Void> withdraw(Long userId,
                                  @RequestBody LogoutRequest token,
                                  @RequestHeader("Authorization") String authorization);
}
