package com.sodosiro.domain.travel.service;

import com.sodosiro.domain.travel.controller.dto.CursorPageResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotDetailResponse;
import com.sodosiro.domain.travel.controller.dto.TouristSpotSummaryResponse;
import com.sodosiro.domain.travel.controller.dto.TravelSpotSort;
import com.sodosiro.domain.like.repository.SpotLikeRepository;
import com.sodosiro.domain.region.service.RegionNameResolver;
import com.sodosiro.domain.review.controller.dto.response.ReviewResponse;
import com.sodosiro.domain.review.entity.Review;
import com.sodosiro.domain.review.entity.ReviewImage;
import com.sodosiro.domain.review.repository.ReviewImageRepository;
import com.sodosiro.domain.review.repository.ReviewRepository;
import com.sodosiro.domain.travel.entity.SpotPopularity;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TravelSpotQueryRepository;
import com.sodosiro.domain.travel.repository.SpotPopularityRepository;
import com.sodosiro.domain.user.entity.User;
import com.sodosiro.domain.user.repository.UserRepository;
import com.sodosiro.global.payload.code.error.TravelErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelSpotService {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 10000;

    private final TravelSpotQueryRepository queryRepository;
    private final SpotLikeRepository spotLikeRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final UserRepository userRepository;
    private final SpotPopularityRepository spotPopularityRepository;
    private final SpotAiRecommendationService spotAiRecommendationService;
    private final SpotRelatedRecommendationService spotRelatedRecommendationService;
    private final RegionNameResolver regionNameResolver;

    public CursorPageResponse<TouristSpotSummaryResponse> getTouristSpots(
            String cursor, Integer size, List<Integer> categories, String keyword,String sigunguCode,
            TravelSpotSort sort, Long userId) {
        int pageSize = normalizePageSize(size);
        TouristSpotCursor parsedCursor = parseTouristSpotCursor(cursor, sort);
        List<TravelSpotQueryRepository.TouristSpotWithPopularity> rows = queryRepository.findTouristSpots(
                sort, parsedCursor.contentId(), parsedCursor.popularityScore(), pageSize, categories, keyword,sigunguCode);
        boolean hasNext = rows.size() > pageSize;
        Set<Long> likedContentIds = findLikedContentIds(userId, rows, pageSize);
        List<TravelSpotQueryRepository.TouristSpotWithPopularity> pageRows = rows.stream().limit(pageSize).toList();
        Map<Long, String> regionByContentId = regionNameResolver.resolveByContentId(
                pageRows.stream().map(TravelSpotQueryRepository.TouristSpotWithPopularity::spot).toList());
        List<TouristSpotSummaryResponse> items = pageRows.stream()
                .map(row -> TouristSpotSummaryResponse.from(
                        row.spot(), toPopularity(row.popularity()),
                        likedContentIds.contains(row.spot().getContentId()),
                        regionByContentId.get(row.spot().getContentId()),
                        parseTags(row.keywordText())))
                .toList();
        String nextCursor = hasNext ? encodeCursor(items.getLast(), sort) : null;
        return new CursorPageResponse<>(items, nextCursor, hasNext);
    }

    public TouristSpotDetailResponse getTouristSpotDetail(Long contentId, Long loginUserId) {
        TouristSpot spot = findTouristSpotDetail(contentId);
        SpotAiRecommendationService.Recommendation recommendation = spotAiRecommendationService.getCached(spot);
        TouristSpotDetailResponse.AiRecommendation aiRecommendation = recommendation == null
                ? TouristSpotDetailResponse.AiRecommendation.unavailable()
                : TouristSpotDetailResponse.AiRecommendation.available(recommendation.reason());
        List<Review> latestReviews = reviewRepository
                .findTop3ByContentIdAndIsDeletedFalseOrderByCreatedAtDesc(contentId);
        TouristSpotDetailResponse.Popularity popularity = spotPopularityRepository.findById(contentId)
                .map(TouristSpotDetailResponse.Popularity::from)
                .orElse(null);
        boolean liked = loginUserId != null
                && spotLikeRepository.findByUserIdAndContentId(loginUserId, contentId).isPresent();
        List<TouristSpotSummaryResponse> relatedSpots = spotRelatedRecommendationService
                .getRecommendations(spot, loginUserId);
        return TouristSpotDetailResponse.from(
                spot, popularity, aiRecommendation, relatedSpots,
                toLatestReviewResponses(spot, latestReviews, loginUserId), liked);
    }

    @Transactional
    public TouristSpotDetailResponse.AiRecommendation generateAiRecommendation(Long contentId) {
        TouristSpot spot = findTouristSpotDetail(contentId);
        SpotAiRecommendationService.Recommendation recommendation = spotAiRecommendationService.getOrGenerate(spot);
        return TouristSpotDetailResponse.AiRecommendation.available(recommendation.reason());
    }

    private TouristSpot findTouristSpotDetail(Long contentId) {
        return queryRepository.findTouristSpotDetail(contentId)
                .orElseThrow(() -> new GeneralException(TravelErrorCode._TOURIST_SPOT_NOT_FOUND));
    }

    private List<ReviewResponse> toLatestReviewResponses(
            TouristSpot spot, List<Review> reviews, Long loginUserId) {
        if (reviews.isEmpty()) {
            return List.of();
        }
        List<Long> userIds = reviews.stream().map(Review::getUserId).distinct().toList();
        Map<Long, User> users = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getUserId, Function.identity()));
        List<Long> reviewIds = reviews.stream().map(Review::getId).toList();
        Map<Long, List<ReviewImage>> imagesByReviewId = reviewImageRepository
                .findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc(reviewIds).stream()
                .collect(Collectors.groupingBy(ReviewImage::getReviewId));

        return reviews.stream()
                .map(review -> ReviewResponse.of(
                        review,
                        users.get(review.getUserId()),
                        spot,
                        imagesByReviewId.getOrDefault(review.getId(), List.of()),
                        loginUserId))
                .toList();
    }

    private int normalizePageSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new GeneralException(TravelErrorCode._INVALID_PAGE_SIZE);
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
            throw new GeneralException(TravelErrorCode._INVALID_CURSOR);
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
            throw new GeneralException(TravelErrorCode._INVALID_CURSOR);
        }
    }

    private String encodeCursor(TouristSpotSummaryResponse spot, TravelSpotSort sort) {
        if (sort != TravelSpotSort.POPULAR) {
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

    /**
     * keyword_text 를 ',' 로 분리해 첫 토큰(대표 분류)을 제외한 나머지 태그 목록을 반환한다.
     */
    private List<String> parseTags(String keywordText) {
        if (keywordText == null || keywordText.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keywordText.split(","))
                .map(String::trim)
                .filter(token -> !token.isEmpty())
                .skip(1)
                .toList();
    }

    private record TouristSpotCursor(Long contentId, Double popularityScore) {
    }
}
