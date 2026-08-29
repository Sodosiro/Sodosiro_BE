package com.sodosiro.domain.auth.oauth.validator;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.sodosiro.domain.auth.contoller.dto.response.SocialUserInfo;
import com.sodosiro.domain.user.constants.Provider;
import com.sodosiro.global.payload.code.error.UserErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

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

    @Value("${spring.security.oauth2.client.registration.kakao.client-secret}")
    private String kakaoClientSecret;

    @Value("${spring.security.oauth2.client.registration.kakao.web-redirect-uri}")
    private String webRedirectUri;

    private static final String KAKAO_ISSUER = "https://kauth.kakao.com";
    private static final String KAKAO_TOKEN_URI = "https://kauth.kakao.com/oauth/token";
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
        } catch (GeneralException e) {
            throw e;
        } catch (Exception e) {
            throw new GeneralException(UserErrorCode._SOCIAL_VERIFICATION_FAILED);
        }
    }

    @Override
    public Map<String, String> exchangeCodeForTokens(String authorizationCode) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", kakaoClientId);
        params.add("client_secret", kakaoClientSecret);
        params.add("redirect_uri", webRedirectUri);
        params.add("code", authorizationCode);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        Map<String, Object> body;
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(KAKAO_TOKEN_URI, request, Map.class);
            body = response.getBody();
        } catch (RestClientException e) {
            log.error("Kakao 인가코드 교환 실패", e);
            throw new GeneralException(UserErrorCode._SOCIAL_CODE_EXCHANGE_FAILED);
        }

        if (body == null || body.get("id_token") == null) {
            throw new GeneralException(UserErrorCode._SOCIAL_CODE_EXCHANGE_FAILED);
        }

        Object refreshToken = body.get("refresh_token");

        return Map.of(
                "id_token", String.valueOf(body.get("id_token")),
                "refresh_token", refreshToken == null ? "" : String.valueOf(refreshToken)
        );
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