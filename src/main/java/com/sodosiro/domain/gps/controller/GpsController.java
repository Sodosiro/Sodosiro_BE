package com.sodosiro.domain.gps.controller;

import com.sodosiro.domain.gps.controller.dto.request.GpsRequest;
import com.sodosiro.domain.gps.controller.dto.response.GpsResponse;
import com.sodosiro.domain.gps.controller.specification.GpsSpecification;
import com.sodosiro.domain.gps.service.GpsService;
import com.sodosiro.global.resolver.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/gps")
public class GpsController implements GpsSpecification {

    private final GpsService gpsService;

    @PostMapping
    public ResponseEntity<GpsResponse> verify(@LoginUser Long userId, @RequestBody @Valid GpsRequest request) {
        return ResponseEntity.ok(gpsService.verify(userId, request));
    }
}
