package com.sodosiro.domain.travel.controller;

import com.sodosiro.domain.travel.controller.dto.CursorPageResponse;
import com.sodosiro.domain.travel.controller.dto.KakaoSpotResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotDetailResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotSummaryResponse;
import com.sodosiro.domain.travel.docs.TravelSpotSpecification;
import com.sodosiro.domain.travel.service.TravelSpotService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/travel")
public class TravelSpotController implements TravelSpotSpecification {

    private final TravelSpotService travelSpotService;

    /**
     * 일반 TourAPI 여행지 목록. category를 생략하면 전체를, 반복 전달하면 복수 카테고리를 조회한다.
     * keyword는 제목에 대한 대소문자 비구분 LIKE 검색이다.
     */
    @GetMapping("/spots")
    @Override
    public ResponseEntity<CursorPageResponse<TouristSpotSummaryResponse>> getTouristSpots(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(name = "category", required = false) List<Integer> categories,
            @RequestParam(required = false) String keyword) {
        return ResponseEntity.ok(travelSpotService.getTouristSpots(cursor, size, categories, keyword));
    }

    /** 상세 조회에서만 이미지 목록을 fetch join한다. */
    @GetMapping("/spots/{contentId}")
    @Override
    public ResponseEntity<TouristSpotDetailResponse> getTouristSpotDetail(@PathVariable Long contentId) {
        return ResponseEntity.ok(travelSpotService.getTouristSpotDetail(contentId));
    }

    /** ETL 카카오 검색 기반 인기 장소. 일반 여행지 목록과 데이터 원천이 달라 별도 제공한다. */
    @GetMapping("/popular-spots")
    @Override
    public ResponseEntity<CursorPageResponse<KakaoSpotResponse>> getPopularSpots(
            @RequestParam(required = false) String cursor,
            @RequestParam(required = false) Integer size,
            @RequestParam(name = "categoryGroupCode", required = false) List<String> categoryGroupCodes) {
        return ResponseEntity.ok(travelSpotService.getPopularSpots(cursor, size, categoryGroupCodes));
    }
}
