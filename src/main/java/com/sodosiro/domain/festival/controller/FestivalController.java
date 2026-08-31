package com.sodosiro.domain.festival.controller;

import com.sodosiro.domain.festival.controller.dto.FestivalDetailResponse;
import com.sodosiro.domain.festival.controller.dto.FestivalStatus;
import com.sodosiro.domain.festival.controller.dto.FestivalSummaryResponse;
import com.sodosiro.domain.festival.docs.FestivalSpecification;
import com.sodosiro.domain.festival.service.FestivalService;
import com.sodosiro.domain.travel.controller.dto.CursorPageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/festivals")
public class FestivalController implements FestivalSpecification {

    private final FestivalService festivalService;

    @GetMapping
    @Override
    public ResponseEntity<CursorPageResponse<FestivalSummaryResponse>> getFestivals(
            @RequestParam(required = false) String areaCode,
            @RequestParam(defaultValue = "ALL") FestivalStatus status,
            @RequestParam(required = false) String reginName,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size) {
        return ResponseEntity.ok(festivalService.getFestivals(areaCode, status, reginName, year, cursor, size));
    }

    @Deprecated
    @GetMapping("/{festivalId}")
    @Override
    public ResponseEntity<FestivalDetailResponse> getFestival(@PathVariable Long festivalId) {
        return ResponseEntity.ok(festivalService.getFestival(festivalId));
    }
}
