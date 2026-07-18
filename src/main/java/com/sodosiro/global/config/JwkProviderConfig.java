package com.sodosiro.global.config;

import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.concurrent.TimeUnit;

@Configuration
public class JwkProviderConfig {

    private static final String KAKAO_JWKS_URL = "https://kauth.kakao.com/.well-known/jwks.json";

    @Bean
    public JwkProvider kakaoJwkProvider() throws MalformedURLException {
        return new JwkProviderBuilder(URI.create(KAKAO_JWKS_URL).toURL())
                .cached(10, 24, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .timeouts(10, 10) // 아까 말한 타임아웃
                .build();
    }
}
