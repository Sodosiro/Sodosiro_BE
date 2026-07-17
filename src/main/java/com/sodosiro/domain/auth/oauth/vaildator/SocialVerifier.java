package com.sodosiro.domain.auth.oauth.vaildator;

import com.sodosiro.domain.auth.dto.response.SocialUserInfo;
import com.sodosiro.domain.user.constants.Provider;

import java.util.HashMap;
import java.util.Map;

public interface SocialVerifier {

    Provider getProvider();
    SocialUserInfo verify(String idToken);
    default Map<String, String> exchangeCodeForTokens(String authorizationCode) {
        return new HashMap<>();
    }
    void unlink(String providerId, String refreshToken);

}