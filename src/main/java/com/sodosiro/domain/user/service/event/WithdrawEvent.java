package com.sodosiro.domain.user.service.event;

import com.sodosiro.domain.user.constants.Provider;

import java.util.List;

public record WithdrawEvent(
        Long userId,
        String accessToken,
        String refreshToken,
        List<SocialInfo> socials
) {
    public record SocialInfo(Provider provider, String providerId, String refreshToken) {}
}
