package com.sodosiro.domain.auth.dto.request;

import lombok.Getter;

@Getter
public class SocialLoginRequest {
    private String provider;
    private String idToken;
    private String authorizationCode;
}

