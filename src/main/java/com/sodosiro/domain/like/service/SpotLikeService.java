package com.sodosiro.domain.like.service;

import com.sodosiro.domain.like.controller.dto.response.LikeToggleResponse;
import com.sodosiro.domain.like.controller.dto.response.MyLikedSpotItem;
import com.sodosiro.domain.like.controller.dto.response.MyLikedSpotListResponse;
import com.sodosiro.domain.like.entity.SpotLike;
import com.sodosiro.domain.like.repository.SpotLikeRepository;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.payload.code.error.LikeErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        TouristSpot spot = touristSpotRepository.findById(contentId)
                .orElseThrow(() -> new GeneralException(LikeErrorCode._TOURIST_SPOT_NOT_FOUND));

        Optional<SpotLike> existing = spotLikeRepository.findByUserIdAndContentId(userId, contentId);
        if (existing.isPresent()) {
            spotLikeRepository.delete(existing.get());
            touristSpotRepository.decrementLikeCount(contentId);
            return new LikeToggleResponse(false, spot.getLikeCount() - 1);
        }

        spotLikeRepository.save(SpotLike.ofTourist(userId, contentId));
        touristSpotRepository.incrementLikeCount(contentId);
        return new LikeToggleResponse(true, spot.getLikeCount() + 1);
    }

    @Transactional(readOnly = true)
    public MyLikedSpotListResponse getMyLikedSpots(Long userId, Long cursor, int size) {
        long effectiveCursor = cursor == null ? CURSOR_START : cursor;

        List<SpotLike> fetched = spotLikeRepository.findByUserId(userId, effectiveCursor, size + 1);

        boolean hasNext = fetched.size() > size;
        List<SpotLike> likes = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = hasNext && !likes.isEmpty() ? likes.getLast().getId() : null;

        List<Long> contentIds = likes.stream()
                .map(SpotLike::getContentId)
                .toList();

        Map<Long, TouristSpot> touristMap = touristSpotRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));

        List<MyLikedSpotItem> items = likes.stream()
                .map(l -> MyLikedSpotItem.from(l, touristMap.get(l.getContentId())))
                .toList();

        return new MyLikedSpotListResponse(items, nextCursor, hasNext);
    }

    @Transactional(readOnly = true)
    public MyLikedSpotListResponse getMyLikedSpotsByRegion(Long userId, String sigunguCode, Long cursor, int size) {
        long effectiveCursor = cursor == null ? CURSOR_START : cursor;

        List<SpotLike> fetched = spotLikeRepository.findByUserIdAndSigunguCode(userId, sigunguCode, effectiveCursor, size + 1);

        boolean hasNext = fetched.size() > size;
        List<SpotLike> likes = hasNext ? fetched.subList(0, size) : fetched;
        Long nextCursor = hasNext && !likes.isEmpty() ? likes.getLast().getId() : null;

        List<Long> contentIds = likes.stream()
                .map(SpotLike::getContentId)
                .toList();

        Map<Long, TouristSpot> touristMap = touristSpotRepository.findAllById(contentIds).stream()
                .collect(Collectors.toMap(TouristSpot::getContentId, Function.identity()));

        List<MyLikedSpotItem> items = likes.stream()
                .map(l -> MyLikedSpotItem.from(l, touristMap.get(l.getContentId())))
                .toList();

        return new MyLikedSpotListResponse(items, nextCursor, hasNext);
    }
}
