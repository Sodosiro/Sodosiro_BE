package com.sodosiro.domain.auth.contoller.dto.response;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.sodosiro.domain.user.constants.Provider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@AllArgsConstructor
@Data
@Builder
public class SocialUserInfo {
    private String email;
    private String nickname;
    private String providerId;
    private Provider provider;

    public static SocialUserInfo of(DecodedJWT jwt, String nickname, Provider provider) {
        return SocialUserInfo.builder()
                .email(jwt.getClaim("email").asString())
                .nickname(nickname)
                .providerId(jwt.getSubject())
                .provider(provider)
                .build();
    }

}
