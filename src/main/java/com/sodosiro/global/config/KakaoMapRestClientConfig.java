package com.sodosiro.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class KakaoMapRestClientConfig {

    private static final String KAKAO_MAP_BASE_URL = "https://dapi.kakao.com";

    @Value("${kakao.map.rest-api-key}")
    private String kakaoMapRestApiKey;

    @Bean
    public RestClient kakaoMapRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return builder
                .baseUrl(KAKAO_MAP_BASE_URL)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoMapRestApiKey)
                .build();
    }
}
