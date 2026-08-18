package com.sodosiro.domain.auth.contoller.dto.request;


public record SocialLoginRequest (
        String provider,
        String idToken,
        String authorizationCode){

}

