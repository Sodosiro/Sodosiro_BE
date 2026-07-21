package com.sodosiro.domain.auth.oauth.validator;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.security.interfaces.RSAPublicKey;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Component
public class KakaoTokenVerifier implements SocialVerifier{

    private final JwkProvider kakaoJwkProvider;

    @Value("${spring.security.oauth2.client.registration.kakao.client-id}")
    private String kakaoClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.native-client-id}")
    private String kakaoNativeClientId;

    @Value("${spring.security.oauth2.client.registration.kakao.admin-key}")
    private String adminKey;

    private static final String KAKAO_ISSUER = "https://kauth.kakao.com";
    private final RestTemplate restTemplate;


    @Override
    public Provider getProvider() {
        return Provider.KAKAO;
    }

    @Override
    public SocialUserInfo verify(String idToken) {
        try {
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

            Jwk jwk = kakaoJwkProvider.get(kid);
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

        if (providerId == null || providerId.isBlank()) {
            return;
        }

        String url = "https://kapi.kakao.com/v1/user/unlink";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + adminKey);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("target_id_type", "user_id");
        params.add("target_id", providerId);

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        try {
            restTemplate.postForEntity(url, request, String.class);
        } catch (Exception e) {
            log.error("Kakao unlink 실패", e);
        }
    }

}