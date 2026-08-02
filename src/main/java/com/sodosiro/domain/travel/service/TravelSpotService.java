package com.sodosiro.domain.travel.service;

import com.sodosiro.domain.travel.controller.dto.CursorPageResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotDetailResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotSummaryResponse;
import com.sodosiro.domain.travel.controller.dto.TravelSpotSort;
import com.sodosiro.domain.like.repository.SpotLikeRepository;
import com.sodosiro.domain.travel.entity.SpotPopularity;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TravelSpotQueryRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Set;
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
    private final SpotLikeRepository spotLikeRepository;

    public CursorPageResponse<TouristSpotSummaryResponse> getTouristSpots(
            String cursor, Integer size, List<Integer> categories, String keyword,
            TravelSpotSort sort, Long userId) {
        int pageSize = normalizePageSize(size);
        TouristSpotCursor parsedCursor = parseTouristSpotCursor(cursor, sort);
        List<TravelSpotQueryRepository.TouristSpotWithPopularity> rows = queryRepository.findTouristSpots(
                sort, parsedCursor.contentId(), parsedCursor.popularityScore(), pageSize, categories, keyword);
        boolean hasNext = rows.size() > pageSize;
        Set<Long> likedContentIds = findLikedContentIds(userId, rows, pageSize);
        List<TouristSpotSummaryResponse> items = rows.stream()
                .limit(pageSize)
                .map(row -> TouristSpotSummaryResponse.from(
                        row.spot(), toPopularity(row.popularity()),
                        likedContentIds.contains(row.spot().getContentId())))
                .toList();
        String nextCursor = hasNext ? encodeCursor(items.getLast(), sort) : null;
        return new CursorPageResponse<>(items, nextCursor, hasNext);
    }

    public TouristSpotDetailResponse getTouristSpotDetail(Long contentId) {
        TouristSpot spot = queryRepository.findTouristSpotDetail(contentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "여행지를 찾을 수 없습니다."));
        return TouristSpotDetailResponse.from(spot);
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

    private TouristSpotCursor parseTouristSpotCursor(String cursor, TravelSpotSort sort) {
        if (cursor == null || cursor.isBlank()) {
            return new TouristSpotCursor(null, null);
        }
        if (sort == TravelSpotSort.POPULAR) {
            return parsePopularityCursor(cursor);
        }
        try {
            return new TouristSpotCursor(Long.valueOf(cursor), null);
        } catch (NumberFormatException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 cursor입니다.");
        }
    }

    private TouristSpotCursor parsePopularityCursor(String cursor) {
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8);
            String[] values = decoded.split(":", -1);
            if (values.length != 2) {
                throw new IllegalArgumentException();
            }
            return new TouristSpotCursor(Long.valueOf(values[1]), Double.valueOf(values[0]));
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "유효하지 않은 cursor입니다.");
        }
    }

    private String encodeCursor(TouristSpotSummaryResponse spot, TravelSpotSort sort) {
        if (sort == TravelSpotSort.DEFAULT) {
            return String.valueOf(spot.contentId());
        }
        String value = spot.popularity().score() + ":" + spot.contentId();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private TouristSpotSummaryResponse.Popularity toPopularity(SpotPopularity popularity) {
        if (popularity == null) {
            return null;
        }
        return new TouristSpotSummaryResponse.Popularity(
                popularity.getPopularityScore(), popularity.getCategoryRank(),
                popularity.getRankTag(), popularity.getCalculatedAt());
    }

    private Set<Long> findLikedContentIds(
            Long userId, List<TravelSpotQueryRepository.TouristSpotWithPopularity> rows, int pageSize) {
        if (userId == null || rows.isEmpty()) {
            return Set.of();
        }
        List<Long> contentIds = rows.stream()
                .limit(pageSize)
                .map(row -> row.spot().getContentId())
                .toList();
        return spotLikeRepository.findLikedContentIdsByUserIdAndContentIds(userId, contentIds);
    }

    private record TouristSpotCursor(Long contentId, Double popularityScore) {
    }
}
