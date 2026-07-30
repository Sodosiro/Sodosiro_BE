package com.sodosiro.domain.travel.service;

import com.sodosiro.domain.travel.controller.dto.CursorPageResponse;
import com.sodosiro.domain.travel.controller.dto.KakaoSpotResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotDetailResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotSummaryResponse;
import com.sodosiro.domain.travel.entity.KakaoSpot;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TravelSpotQueryRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelSpotService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;

    private final TravelSpotQueryRepository queryRepository;

    public CursorPageResponse<TouristSpotSummaryResponse> getTouristSpots(
            String cursor, Integer size, List<Integer> categories, String keyword) {
        int pageSize = normalizePageSize(size);
        List<TouristSpot> rows = queryRepository.findTouristSpots(parseTouristCursor(cursor), pageSize, categories, keyword);
        boolean hasNext = rows.size() > pageSize;
        List<TouristSpotSummaryResponse> items = rows.stream()
                .limit(pageSize)
                .map(TouristSpotSummaryResponse::from)
                .toList();
        String nextCursor = hasNext ? String.valueOf(items.getLast().contentId()) : null;
        return new CursorPageResponse<>(items, nextCursor, hasNext);
    }

    public TouristSpotDetailResponse getTouristSpotDetail(Long contentId) {
        TouristSpot spot = queryRepository.findTouristSpotDetail(contentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행지를 찾을 수 없습니다."));
        return TouristSpotDetailResponse.from(spot);
    }

    public CursorPageResponse<KakaoSpotResponse> getPopularSpots(
            String cursor, Integer size, List<String> categoryGroupCodes) {
        int pageSize = normalizePageSize(size);
        PopularSpotCursor parsedCursor = parsePopularSpotCursor(cursor);
        List<KakaoSpot> rows = queryRepository.findPopularSpots(
                parsedCursor.score(), parsedCursor.id(), pageSize, categoryGroupCodes);
        boolean hasNext = rows.size() > pageSize;
        List<KakaoSpotResponse> items = rows.stream().limit(pageSize).map(KakaoSpotResponse::from).toList();
        String nextCursor = hasNext ? encodePopularSpotCursor(items.getLast()) : null;
        return new CursorPageResponse<>(items, nextCursor, hasNext);
    }

    private int normalizePageSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size는 1~100 사이여야 합니다.");
        }
        return size;
    }

    private Long parseTouristCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(cursor);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 cursor입니다.");
        }
    }

    private PopularSpotCursor parsePopularSpotCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return new PopularSpotCursor(null, null);
        }
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] values = decoded.split(":", -1);
            if (values.length != 2) {
                throw new IllegalArgumentException();
            }
            return new PopularSpotCursor(Double.valueOf(values[0]), Long.valueOf(values[1]));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 cursor입니다.");
        }
    }

    private String encodePopularSpotCursor(KakaoSpotResponse spot) {
        String value = spot.popularityScore() + ":" + spot.id();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private record PopularSpotCursor(Double score, Long id) {
    }
}
