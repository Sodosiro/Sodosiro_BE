package com.sodosiro.domain.auth.specification;

import com.sodosiro.domain.auth.dto.request.SocialLoginRequest;
import com.sodosiro.domain.auth.dto.response.SocialLoginResponse;
import com.sodosiro.global.payload.code.error.CommonErrorCode;
import com.sodosiro.global.payload.code.error.UserErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.annotation.security.PermitAll;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

public interface AuthSpecification {

    @PermitAll
    @Operation(summary = "소셜 SDK 토큰 검증 후 jwt 발급",
            description = "토큰 만료일 : refresh: 7일 , access: 15분")
    public ResponseEntity<SocialLoginResponse> socialLogin(@RequestBody SocialLoginRequest request);

}
