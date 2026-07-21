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

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/etl/travel")
public class DataExtractEtlController {

    private final DataExtractRefreshService dataExtractRefreshService;

    @PostMapping("/refresh")
    public ResponseEntity<TravelRefreshResponse> refresh(@RequestBody TravelRefreshRequest request) {
        int accepted = dataExtractRefreshService.accept(request.runId(), request.contentIds());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(new TravelRefreshResponse(accepted));
    }
}
