package com.sodosiro.domain.user.service.event;

public record WithdrawEvent(
        Long userId,
        String accessToken,
        String refreshToken
) {}
