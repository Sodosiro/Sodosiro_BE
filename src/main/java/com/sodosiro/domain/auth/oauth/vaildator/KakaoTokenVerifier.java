package com.sodosiro.domain.auth.oauth.vaildator;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.sodosiro.domain.auth.dto.response.SocialUserInfo;
import com.sodosiro.domain.user.constants.Provider;
import com.sodosiro.global.payload.code.error.UserErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

@RequiredArgsConstructor
@Component
public class KakaoTokenVerifier implements SocialVerifier{

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.native-client-id}")
    private String kakaoNativeClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.admin-key}")
    private String adminKey;

    private static final String KAKAO_JWKS_URL = "https://kauth.kakao.com/.well-known/jwks.json";
    private static final String KAKAO_ISSUER = "https://kauth.kakao.com";

    @Override
    public Provider getProvider() {
        return Provider.KAKAO;
    }

    @Override
    public SocialUserInfo verify(String idToken) {
        try {
            UrlJwkProvider provider = new UrlJwkProvider(new URL(KAKAO_JWKS_URL));

            DecodedJWT decodedJWT = JWT.decode(idToken);
            String kid = decodedJWT.getKeyId();
            List<String> audiences = decodedJWT.getAudience();

            if (audiences == null || audiences.isEmpty()) {
                throw new GeneralException(UserErrorCode._SOCIAL_ID_TOKEN_INVALID);
            }

            String aud = audiences.get(0);
            String expectedAud;
            if (aud.equals(kakaoNativeClientId)) {
                expectedAud = kakaoNativeClientId;
            } else if (aud.equals(kakaoClientId)) {
                expectedAud = kakaoClientId;
            } else {
                throw new GeneralException(UserErrorCode._SOCIAL_TOKEN_INVALID_AUDIENCE);
            }

            Jwk jwk = provider.get(kid);
            Algorithm algorithm = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);

            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(KAKAO_ISSUER)
                    .withAudience(expectedAud)
                    .build();

            DecodedJWT verified;

            try {
                verified = verifier.verify(idToken);
            } catch (TokenExpiredException e) {
                throw new GeneralException(UserErrorCode._SOCIAL_TOKEN_EXPIRED);
            } catch (SignatureVerificationException e) {
                throw new GeneralException(UserErrorCode._SOCIAL_TOKEN_INVALID_SIGNATURE);
            } catch (Exception e) {
                throw new GeneralException(UserErrorCode._SOCIAL_VERIFICATION_FAILED);
            }

            String nickname = verified.getClaim("nickname").asString();

            return SocialUserInfo.of(verified, nickname, getProvider());

        } catch (Exception e) {
            throw new GeneralException(UserErrorCode._SOCIAL_VERIFICATION_FAILED);
        }
    }

    @Override
    public void unlink(String providerId, String refreshToken) {

    }

}