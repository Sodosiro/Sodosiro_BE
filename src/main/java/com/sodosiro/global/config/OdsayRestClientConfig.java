package com.sodosiro.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class OdsayRestClientConfig {

    private static final String ODSAY_BASE_URL = "https://api.odsay.com";

    @Bean
    public RestClient odsayRestClient(RestClient.Builder builder) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return builder
                .baseUrl(ODSAY_BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }
}
