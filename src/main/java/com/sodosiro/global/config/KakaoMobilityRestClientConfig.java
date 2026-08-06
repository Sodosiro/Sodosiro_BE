package com.sodosiro.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class KakaoMobilityRestClientConfig {

    private static final String KAKAO_MOBILITY_BASE_URL = "https://apis-navi.kakaomobility.com";

    @Value("${kakao.mobility.rest-api-key}")
    private String kakaoMobilityRestApiKey;

    @Bean
    public RestClient kakaoMobilityRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return builder
                .baseUrl(KAKAO_MOBILITY_BASE_URL)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + kakaoMobilityRestApiKey)
                .build();
    }
}
