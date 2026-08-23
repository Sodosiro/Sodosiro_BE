package com.sodosiro.domain.region.controller;

import com.sodosiro.domain.region.controller.dto.RegionIntroductionResponse;
import com.sodosiro.domain.region.controller.dto.RegionCodeResponse;
import com.sodosiro.domain.region.controller.dto.AreaCodeResponse;
import com.sodosiro.domain.region.controller.dto.VisitedRegionResponse;
import com.sodosiro.domain.region.controller.specification.RegionIntroSpecification;
import com.sodosiro.domain.region.service.RegionIntroService;
import com.sodosiro.domain.region.service.RegionDirectoryService;
import com.sodosiro.domain.region.service.VisitedRegionService;
import com.sodosiro.global.resolver.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/regions")
public class RegionIntroController implements RegionIntroSpecification {

    private final RegionIntroService regionIntroService;
    private final RegionDirectoryService regionDirectoryService;
    private final VisitedRegionService visitedRegionService;

    @GetMapping("/areas")
    @Override
    public ResponseEntity<List<AreaCodeResponse>> getAreas() {
        return ResponseEntity.ok(regionDirectoryService.getAreas());
    }

    @GetMapping
    @Override
    public ResponseEntity<java.util.List<RegionCodeResponse>> getRegions(
            @RequestParam String areaCode) {
        return ResponseEntity.ok(regionDirectoryService.getRegions(areaCode));
    }

    @GetMapping("/{sigunguId}/introduction")
    @Override
    public ResponseEntity<RegionIntroductionResponse> getIntroduction(
            @PathVariable Long sigunguId) {
        return ResponseEntity.ok(regionIntroService.getIntroduction(sigunguId));
    }

    @GetMapping("/visited")
    @Override
    public ResponseEntity<VisitedRegionResponse> getVisitedRegions(
            @LoginUser Long userId,
            @RequestParam(required = false) String areaCode) {
        return ResponseEntity.ok(visitedRegionService.getVisitedRegions(userId, areaCode));
    }
}
