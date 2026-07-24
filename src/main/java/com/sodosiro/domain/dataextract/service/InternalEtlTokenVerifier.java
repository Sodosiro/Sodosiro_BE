package com.sodosiro.domain.dataextract.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Airflow 등 내부 ETL 호출의 공유 서비스 토큰을 검증한다. */
@Component
public class InternalEtlTokenVerifier {

    private final String expectedToken;

    public InternalEtlTokenVerifier(@Value("${INTERNAL_ETL_TOKEN:}") String expectedToken) {
        this.expectedToken = expectedToken;
    }

    public void verify(String providedToken) {
        if (!StringUtils.hasText(expectedToken)) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "INTERNAL_ETL_TOKEN is not configured");
        }
        if (!StringUtils.hasText(providedToken) || !MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                providedToken.getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid internal ETL token");
        }
    }
}
