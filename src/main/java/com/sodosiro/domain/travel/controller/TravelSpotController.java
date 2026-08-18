package com.sodosiro.domain.travel.controller;

import com.sodosiro.domain.travel.controller.dto.CursorPageResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotDetailResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotSummaryResponse;
import com.sodosiro.domain.travel.controller.dto.TrendingKeywordResponse;
import com.sodosiro.domain.travel.controller.dto.TravelSpotSort;
import com.sodosiro.domain.travel.docs.TravelSpotSpecification;
import com.sodosiro.domain.travel.service.SearchTrendingService;
import com.sodosiro.domain.travel.service.TravelSpotService;
import com.sodosiro.domain.travel.service.event.SearchKeywordSearchedEvent;
import com.sodosiro.global.resolver.LoginUser;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/travel")
public class TravelSpotController implements TravelSpotSpecification {

    private final TravelSpotService travelSpotService;
    private final SearchTrendingService searchTrendingService;
    private final ApplicationEventPublisher eventPublisher;

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
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "DEFAULT") TravelSpotSort sort,
            @LoginUser Long userId) {
        if (keyword != null && !keyword.isBlank()) {
            eventPublisher.publishEvent(new SearchKeywordSearchedEvent(keyword, userId));
        }
        return ResponseEntity.ok(travelSpotService.getTouristSpots(cursor, size, categories, keyword, sort, userId));
    }

    /** 상세 조회에서만 이미지 목록을 fetch join한다. */
    @GetMapping("/spots/{contentId}")
    @Override
    public ResponseEntity<TouristSpotDetailResponse> getTouristSpotDetail(
            @PathVariable Long contentId, @LoginUser Long userId) {
        return ResponseEntity.ok(travelSpotService.getTouristSpotDetail(contentId, userId));
    }

    @PostMapping("/spots/{contentId}/ai-recommendation")
    @Override
    public ResponseEntity<TouristSpotDetailResponse.AiRecommendation> generateAiRecommendation(
            @PathVariable Long contentId) {
        return ResponseEntity.ok(travelSpotService.generateAiRecommendation(contentId));
    }

    @GetMapping("/search/trending")
    public ResponseEntity<List<TrendingKeywordResponse>> getSearchTrending() {
        return ResponseEntity.ok(searchTrendingService.getTrending());
    }

}
