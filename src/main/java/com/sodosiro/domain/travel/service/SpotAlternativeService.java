package com.sodosiro.domain.travel.service;

import com.sodosiro.domain.like.repository.SpotLikeRepository;
import com.sodosiro.domain.region.service.RegionNameResolver;
import com.sodosiro.domain.travel.controller.dto.TouristSpotSummaryResponse;
import com.sodosiro.domain.travel.entity.SpotEmbedding;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.SpotEmbeddingQueryRepository.RelatedEmbeddingCandidate;
import com.sodosiro.domain.travel.repository.SpotEmbeddingRepository;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.payload.code.error.TravelErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 코스 내 특정 장소를 대체할 후보를 추천한다.
 * 대상 장소와 같은 카테고리 안에서 반경(3km -> 5km -> 10km)을 넓혀가며 후보가 3개 모일 때까지 검색하고,
 * 임베딩 코사인 유사도가 가장 높은 상위 3개를 반환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SpotAlternativeService {

    private static final List<Double> RADIUS_STEPS_KM = List.of(3D, 5D, 10D);
    private static final int ALTERNATIVE_SIZE = 3;

    private final TouristSpotRepository touristSpotRepository;
    private final SpotEmbeddingRepository spotEmbeddingRepository;
    private final SpotLikeRepository spotLikeRepository;
    private final RegionNameResolver regionNameResolver;

    public List<TouristSpotSummaryResponse> getAlternatives(Long targetContentId, Long userId) {

        TouristSpot target = touristSpotRepository.findById(targetContentId)
                .orElseThrow(() -> new GeneralException(TravelErrorCode._TOURIST_SPOT_NOT_FOUND));

        SpotEmbedding targetEmbedding = spotEmbeddingRepository.findById(targetContentId)
                .orElseThrow(() -> new GeneralException(TravelErrorCode._SPOT_EMBEDDING_NOT_FOUND));

        if (target.getMapX() == null || target.getMapY() == null) {
            throw new GeneralException(TravelErrorCode._SPOT_COORDINATE_MISSING);
        }

        Set<Long> excludedIds = new LinkedHashSet<>();
        excludedIds.add(targetContentId);
        List<RelatedEmbeddingCandidate> candidates = new ArrayList<>();

        for (double radiusKm : RADIUS_STEPS_KM) {
            int needCount = ALTERNATIVE_SIZE - candidates.size();

            List<RelatedEmbeddingCandidate> stepCandidates = spotEmbeddingRepository.findAlternativeCandidates(
                    target.getCategory(), target.getMapY(), target.getMapX(),
                    targetEmbedding.getEmbedding(), radiusKm, needCount, excludedIds);

            // 의도적 설계: 사용자 경험을 위해 유사도보다 물리적 거리(반경)를 최우선으로 추천
            // 반경 내에서만 유사도순 정렬을 유지한 채 루프를 돌며 후보 수집
            for (RelatedEmbeddingCandidate candidate : stepCandidates) {
                excludedIds.add(candidate.contentId());
                candidates.add(candidate);
            }

            if (candidates.size() >= ALTERNATIVE_SIZE) {
                break;
            }
        }

        List<Long> ids = candidates.stream().map(RelatedEmbeddingCandidate::contentId).toList();
        return toResponses(ids, userId);
    }

    private List<TouristSpotSummaryResponse> toResponses(List<Long> ids, Long userId) {

        if (ids.isEmpty()) {
            return List.of();
        }

        Map<Long, TouristSpot> spots = touristSpotRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));

        Set<Long> likedIds = userId == null ? Set.of()
                : spotLikeRepository.findLikedContentIdsByUserIdAndContentIds(userId, ids);

        Map<Long, String> regionByContentId = regionNameResolver.resolveByContentId(spots.values());

        return ids.stream()
                .map(spots::get)
                .filter(Objects::nonNull)
                .map(spot -> TouristSpotSummaryResponse.from(
                        spot, null, likedIds.contains(spot.getContentId()), regionByContentId.get(spot.getContentId())))
                .toList();
    }
}
