package com.sodosiro.domain.auth.oauth.validator;

import com.sodosiro.domain.auth.contoller.dto.response.SocialUserInfo;
import com.sodosiro.domain.user.constants.Provider;

import java.util.Collections;
import java.util.Map;

public interface SocialVerifier {

    Provider getProvider();
    SocialUserInfo verify(String idToken);
    default Map<String, String> exchangeCodeForTokens(String authorizationCode) {
        return Collections.emptyMap();
    }

    void unlink(String providerId, String refreshToken);

}