package com.sodosiro.domain.notification.location.controller;

import com.sodosiro.domain.notification.location.LocationUpdateResult;
import com.sodosiro.domain.notification.location.LocationUpdateService;
import com.sodosiro.domain.notification.location.controller.dto.LocationUpdateApiRequest;
import com.sodosiro.domain.notification.location.controller.dto.LocationUpdateResponse;
import com.sodosiro.domain.notification.location.controller.specification.LocationSpecification;
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
@RequestMapping("/api/v1/locations")
public class LocationController implements LocationSpecification {

    private final LocationUpdateService locationUpdateService;

    @PostMapping
    @Override
    public ResponseEntity<LocationUpdateResponse> updateLocation(
            @LoginUser Long userId,
            @Valid @RequestBody LocationUpdateApiRequest request) {
        LocationUpdateResult result = locationUpdateService.process(
                userId, request.latitude(), request.longitude(), request.accuracy(), request.occurredAt());
        return ResponseEntity.ok(LocationUpdateResponse.from(result));
    }
}
