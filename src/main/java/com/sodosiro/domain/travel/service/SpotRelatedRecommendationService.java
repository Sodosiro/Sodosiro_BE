package com.sodosiro.domain.travel.service;

import com.sodosiro.domain.like.repository.SpotLikeRepository;
import com.sodosiro.domain.region.service.RegionNameResolver;
import com.sodosiro.domain.travel.controller.dto.TouristSpotSummaryResponse;
import com.sodosiro.domain.travel.entity.SpotPopularity;
import com.sodosiro.domain.travel.entity.SpotRelatedRecommendation;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.SpotEmbeddingRepository;
import com.sodosiro.domain.travel.repository.SpotEmbeddingQueryRepository;
import com.sodosiro.domain.travel.repository.SpotPopularityRepository;
import com.sodosiro.domain.travel.repository.SpotRelatedRecommendationRepository;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.service.RedisService;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SpotRelatedRecommendationService {

    private static final int RECOMMENDATION_SIZE = 5;
    private static final int VECTOR_CANDIDATE_SIZE = 40;
    private static final Duration TTL = Duration.ofDays(1);
    private static final Duration EMPTY_TTL = Duration.ofMinutes(30);
    private static final String KEY_PREFIX = "travel:spot-related:v1:";

    private final SpotEmbeddingRepository spotEmbeddingRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final SpotPopularityRepository spotPopularityRepository;
    private final SpotRelatedRecommendationRepository snapshotRepository;
    private final SpotRelatedRecommendationSnapshotWriter snapshotWriter;
    private final SpotLikeRepository spotLikeRepository;
    private final RedisService redisService;
    private final RegionNameResolver regionNameResolver;

    public List<TouristSpotSummaryResponse> getRecommendations(TouristSpot source, Long userId) {
        List<Long> cachedIds = getCachedIds(source.getContentId());
        List<Long> relatedIds = cachedIds != null ? cachedIds : generateAndCache(source);
        return toResponses(relatedIds, userId);
    }

    private List<Long> getCachedIds(Long contentId) {
        String key = KEY_PREFIX + contentId;
        List<Long> redisIds = decode(redisService.getValue(key));
        if (redisIds != null) {
            return redisIds;
        }

        LocalDateTime now = LocalDateTime.now();
        SpotRelatedRecommendation snapshot = snapshotRepository
                .findByContentIdAndExpiresAtAfter(contentId, now)
                .orElse(null);
        if (snapshot == null) {
            return null;
        }
        List<Long> ids = decode(snapshot.getRelatedContentIds());
        if (ids != null) {
            cache(key, ids, Duration.between(now, snapshot.getExpiresAt()));
        }
        return ids;
    }

    private List<Long> generateAndCache(TouristSpot source) {
        List<SpotEmbeddingQueryRepository.RelatedEmbeddingCandidate> candidates = spotEmbeddingRepository
                .findRelatedCandidates(source.getContentId(), VECTOR_CANDIDATE_SIZE);
        List<Long> ids = rank(source, candidates);
        LocalDateTime now = LocalDateTime.now();
        Duration ttl = ids.isEmpty() ? EMPTY_TTL : TTL;
        LocalDateTime expiresAt = now.plus(ttl);
        try {
            snapshotWriter.save(SpotRelatedRecommendation.create(source.getContentId(), encode(ids), now, expiresAt));
        } catch (DataIntegrityViolationException concurrentGeneration) {
            // 동시 요청이 먼저 스냅샷을 저장한 경우. 계산된 결과를 그대로 사용한다.
        }
        cache(KEY_PREFIX + source.getContentId(), ids, ttl);
        return ids;
    }

    private List<Long> rank(TouristSpot source,
            List<SpotEmbeddingQueryRepository.RelatedEmbeddingCandidate> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        List<Long> candidateIds = candidates.stream()
                .map(SpotEmbeddingQueryRepository.RelatedEmbeddingCandidate::contentId).toList();
        Map<Long, TouristSpot> spots = byId(touristSpotRepository.findAllById(candidateIds));
        Map<Long, SpotPopularity> popularities = byId(spotPopularityRepository.findAllById(candidateIds));
        double maxLikes = spots.values().stream().mapToDouble(spot -> logCount(spot.getLikeCount())).max().orElse(1D);

        return candidates.stream()
                .map(candidate -> new ScoredCandidate(candidate.contentId(), score(source,
                        spots.get(candidate.contentId()), popularities.get(candidate.contentId()),
                        candidate.distance(), maxLikes)))
                .filter(candidate -> !Double.isNaN(candidate.score()))
                .sorted(Comparator.comparingDouble(ScoredCandidate::score).reversed()
                        .thenComparing(ScoredCandidate::contentId))
                .limit(RECOMMENDATION_SIZE)
                .map(ScoredCandidate::contentId)
                .toList();
    }

    private double score(TouristSpot source, TouristSpot candidate, SpotPopularity popularity,
            Double distance, double maxLikes) {
        if (candidate == null || distance == null) {
            return Double.NaN;
        }
        double semantic = clamp(1D - distance);
        double reviewConfidence = Math.min(1D, Math.log1p(nullToZero(candidate.getReviewCount())) / Math.log(21D));
        double rating = candidate.getAvgRating() == null ? 0D
                : candidate.getAvgRating().doubleValue() / 5D;
        double reviewQuality = rating * reviewConfidence;
        double likes = maxLikes == 0D ? 0D : logCount(candidate.getLikeCount()) / maxLikes;
        double location = locationScore(source, candidate);
        double category = source.getCategory() != null
                && source.getCategory().equals(candidate.getCategory()) ? 0.4D : 1D;
        double popularityBoost = popularity == null ? 0D : Math.min(1D, Math.log1p(Math.max(0D,
                popularity.getPopularityScore())) / Math.log(101D));
        return 0.60D * semantic + 0.15D * reviewQuality + 0.10D * likes
                + 0.10D * location + 0.03D * category + 0.02D * popularityBoost;
    }

    private double locationScore(TouristSpot source, TouristSpot candidate) {
        if (source.getSigunguCode() != null && source.getSigunguCode().equals(candidate.getSigunguCode())) {
            return 1D;
        }
        if (source.getMapX() == null || source.getMapY() == null
                || candidate.getMapX() == null || candidate.getMapY() == null) {
            return 0D;
        }
        return haversineKm(source.getMapY(), source.getMapX(), candidate.getMapY(), candidate.getMapX()) <= 20D ? 0.5D : 0D;
    }

    private List<TouristSpotSummaryResponse> toResponses(List<Long> ids, Long userId) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<Long, TouristSpot> spots = byId(touristSpotRepository.findAllById(ids));
        Map<Long, SpotPopularity> popularities = byId(spotPopularityRepository.findAllById(ids));
        Set<Long> likedIds = userId == null ? Set.of()
                : spotLikeRepository.findLikedContentIdsByUserIdAndContentIds(userId, ids);
        Map<Long, String> regionByContentId = regionNameResolver.resolveByContentId(spots.values());
        return ids.stream().map(spots::get).filter(java.util.Objects::nonNull)
                .map(spot -> TouristSpotSummaryResponse.from(spot, toPopularity(popularities.get(spot.getContentId())),
                        likedIds.contains(spot.getContentId()), regionByContentId.get(spot.getContentId())))
                .toList();
    }

    private TouristSpotSummaryResponse.Popularity toPopularity(SpotPopularity popularity) {
        return popularity == null ? null : new TouristSpotSummaryResponse.Popularity(
                popularity.getPopularityScore(), popularity.getCategoryRank(), popularity.getRankTag(), popularity.getCalculatedAt());
    }

    private void cache(String key, List<Long> ids, Duration ttl) {
        if (!ttl.isNegative() && !ttl.isZero()) {
            redisService.save(key, encode(ids), ttl.toMillis());
        }
    }

    private static List<Long> decode(String value) {
        if (value == null) return null;
        if (value.isBlank()) return List.of();
        try {
            List<Long> ids = new ArrayList<>();
            for (String item : value.split(",")) ids.add(Long.parseLong(item));
            return ids;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static String encode(Collection<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(","));
    }

    private <T> Map<Long, T> byId(Iterable<T> values) {
        Map<Long, T> result = new HashMap<>();
        for (T value : values) {
            if (value instanceof TouristSpot spot) result.put(spot.getContentId(), value);
            if (value instanceof SpotPopularity popularity) result.put(popularity.getContentId(), value);
        }
        return result;
    }

    private double haversineKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1.doubleValue())) * Math.cos(Math.toRadians(lat2.doubleValue()))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 6371D * 2D * Math.atan2(Math.sqrt(a), Math.sqrt(1D - a));
    }

    private double logCount(Integer value) { return Math.log1p(nullToZero(value)); }
    private int nullToZero(Integer value) { return value == null ? 0 : value; }
    private double clamp(double value) { return Math.clamp(value, 0D, 1D); }

    private record ScoredCandidate(Long contentId, double score) { }
}
