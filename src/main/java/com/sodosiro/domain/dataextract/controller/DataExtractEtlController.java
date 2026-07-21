package com.sodosiro.domain.dataextract.controller;

import com.sodosiro.domain.dataextract.controller.dto.TravelRefreshRequest;
import com.sodosiro.domain.dataextract.controller.dto.TravelRefreshResponse;
import com.sodosiro.domain.dataextract.service.DataExtractRefreshService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ETL 서버 전용 엔드포인트.
 *
 * <p>운영 환경에서는 이 경로를 Nginx를 통해서만 Spring으로 프록시한다. Spring 애플리케이션
 * 포트는 외부에 직접 노출하지 않으며, Nginx가 서버 간 인증(mTLS 또는 서비스 토큰)을 검증한
 * 요청만 이 경로로 전달해야 한다.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/etl/travel")
public class DataExtractEtlController {

    private final DataExtractRefreshService dataExtractRefreshService;

    /**
     * Accepts a travel data refresh request for processing.
     *
     * @param request the travel refresh request containing the run identifier and content identifiers
     * @return an accepted response containing the number of refresh items accepted
     */
    @PostMapping("/refresh")
    public ResponseEntity<TravelRefreshResponse> refresh(@RequestBody TravelRefreshRequest request) {
        int accepted = dataExtractRefreshService.accept(request.runId(), request.contentIds());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new TravelRefreshResponse(accepted));
    }
}
