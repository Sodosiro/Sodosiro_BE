package com.sodosiro.domain.auth.dto.request;

public record LogoutRequest(
        String refreshToken
) {
}
