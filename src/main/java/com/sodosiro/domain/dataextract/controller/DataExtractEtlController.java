package com.sodosiro.domain.dataextract.controller;

import com.sodosiro.domain.dataextract.controller.dto.TravelRefreshRequest;
import com.sodosiro.domain.dataextract.controller.dto.TravelRefreshResponse;
import com.sodosiro.domain.dataextract.service.DataExtractRefreshService;
import com.sodosiro.domain.dataextract.service.InternalEtlTokenVerifier;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * ETL 서버 전용 엔드포인트.
 *
 * <p>Airflow는 {@code X-Internal-ETL-Token} 서비스 토큰을 포함해야 한다. 운영 환경에서는
 * 이 경로를 Nginx로 제한하고 Spring 애플리케이션 포트를 외부에 직접 노출하지 않는다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/etl/travel")
public class DataExtractEtlController {

    private final DataExtractRefreshService dataExtractRefreshService;
    private final InternalEtlTokenVerifier internalEtlTokenVerifier;

    @PostMapping("/refresh")
    public ResponseEntity<TravelRefreshResponse> refresh(
            @RequestHeader(value = "X-Internal-ETL-Token", required = false) String internalEtlToken,
            @RequestBody TravelRefreshRequest request) {
        internalEtlTokenVerifier.verify(internalEtlToken);
        int accepted = dataExtractRefreshService.accept(request.runId(), request.contentIds());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new TravelRefreshResponse(accepted));
    }
}
