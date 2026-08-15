package com.sodosiro.domain.like.service;

import com.sodosiro.domain.like.controller.dto.response.LikeToggleResponse;
import com.sodosiro.domain.like.controller.dto.response.MyLikedSpotItem;
import com.sodosiro.domain.like.controller.dto.response.MyLikedSpotListResponse;
import com.sodosiro.domain.like.controller.dto.response.SpotLikeBatchToggleResponse;
import com.sodosiro.domain.like.entity.SpotLike;
import com.sodosiro.domain.like.repository.SpotLikeRepository;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.payload.code.error.LikeErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SpotLikeService {

    private static final long CURSOR_START = Long.MAX_VALUE;

    private final SpotLikeRepository spotLikeRepository;
    private final TouristSpotRepository touristSpotRepository;

    @Transactional
    public LikeToggleResponse toggleTouristSpotLike(Long userId, Long contentId) {
        SpotLikeBatchToggleResponse.Item item = toggleTouristSpotLikes(userId, List.of(contentId)).items().getFirst();
        return new LikeToggleResponse(item.liked(), item.likeCount());
    }

    /** 단건과 여러 건을 하나의 트랜잭션으로 토글한다. 검증 실패 시 어떤 좋아요도 변경하지 않는다. */
    @Transactional
    public SpotLikeBatchToggleResponse toggleTouristSpotLikes(Long userId, List<Long> contentIds) {
        validateUniqueContentIds(contentIds);
        Map<Long, TouristSpot> spotsById = findSpots(contentIds);
        Map<Long, SpotLike> existingLikes = findExistingLikes(userId, contentIds);
        persistLikeChanges(userId, contentIds, existingLikes);
        return new SpotLikeBatchToggleResponse(updateLikeCountsAndBuildItems(contentIds, spotsById, existingLikes));
    }

    private void validateUniqueContentIds(List<Long> contentIds) {
        Set<Long> requestedIds = Set.copyOf(contentIds);
        if (requestedIds.size() != contentIds.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "contentIds에 중복된 관광지가 있습니다.");
        }
    }

    private Map<Long, TouristSpot> findSpots(List<Long> contentIds) {
        Map<Long, TouristSpot> spotsById = touristSpotRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));
        if (spotsById.size() != contentIds.size()) {
            throw new GeneralException(LikeErrorCode._TOURIST_SPOT_NOT_FOUND);
        }
        return spotsById;
    }

    private Map<Long, SpotLike> findExistingLikes(Long userId, List<Long> contentIds) {
        return spotLikeRepository.findByUserIdAndContentIdIn(userId, contentIds).stream()
                .collect(Collectors.toMap(SpotLike::getContentId, Function.identity()));
    }

    private void persistLikeChanges(Long userId, List<Long> contentIds, Map<Long, SpotLike> existingLikes) {
        List<SpotLike> deletes = contentIds.stream().map(existingLikes::get)
                .filter(java.util.Objects::nonNull).toList();
        List<SpotLike> creates = contentIds.stream()
                .filter(contentId -> !existingLikes.containsKey(contentId))
                .map(contentId -> SpotLike.ofTourist(userId, contentId)).toList();
        spotLikeRepository.deleteAll(deletes);
        spotLikeRepository.saveAll(creates);
    }

    private List<SpotLikeBatchToggleResponse.Item> updateLikeCountsAndBuildItems(
            List<Long> contentIds, Map<Long, TouristSpot> spotsById, Map<Long, SpotLike> existingLikes) {
        return contentIds.stream()
                .map(contentId -> updateLikeCountAndBuildItem(contentId, spotsById.get(contentId),
                        existingLikes.containsKey(contentId)))
                .toList();
    }

    private SpotLikeBatchToggleResponse.Item updateLikeCountAndBuildItem(
            Long contentId, TouristSpot spot, boolean wasLiked) {
        if (wasLiked) {
            touristSpotRepository.decrementLikeCount(contentId);
            return new SpotLikeBatchToggleResponse.Item(contentId, false, Math.max(0, spot.getLikeCount() - 1));
        }
        touristSpotRepository.incrementLikeCount(contentId);
        return new SpotLikeBatchToggleResponse.Item(contentId, true, spot.getLikeCount() + 1);
    }


    @Transactional(readOnly = true)
    public MyLikedSpotListResponse getMyLikedSpots(Long userId, String sigunguCode, Long cursor, int size) {
        long effectiveCursor = cursor == null ? CURSOR_START : cursor;

        List<SpotLike> fetched = spotLikeRepository.findByUserIdAndFilters(userId, sigunguCode, effectiveCursor, size + 1);

        boolean hasNext = fetched.size() > size;
        List<SpotLike> likes = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = hasNext && !likes.isEmpty() ? likes.getLast().getId() : null;

        List<MyLikedSpotItem> items = likes.stream()
                .map(l -> MyLikedSpotItem.from(l, l.getTouristSpot()))
                .toList();

        return new MyLikedSpotListResponse(items, nextCursor, hasNext);
    }

}
