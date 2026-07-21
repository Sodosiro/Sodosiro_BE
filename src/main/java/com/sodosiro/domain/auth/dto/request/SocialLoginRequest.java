package com.sodosiro.domain.auth.dto.request;


public record SocialLoginRequest (
        String provider,
        String idToken,
        String authorizationCode){

}

