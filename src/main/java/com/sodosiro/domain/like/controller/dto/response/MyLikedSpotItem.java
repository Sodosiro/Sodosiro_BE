package com.sodosiro.domain.like.controller.dto.response;

import com.sodosiro.domain.like.entity.SpotLike;
import com.sodosiro.domain.travel.entity.KakaoSpot;
import com.sodosiro.domain.travel.entity.TouristSpot;

import java.time.LocalDateTime;

public record MyLikedSpotItem(
        SpotType spotType,
        Long id,
        String name,
        String address,
        String imageUrl,
        Integer likeCount,
        LocalDateTime likedAt
) {
    public enum SpotType { TOURIST, POPULAR }

    public static MyLikedSpotItem fromTourist(SpotLike like, TouristSpot spot) {
        return new MyLikedSpotItem(
                SpotType.TOURIST,
                spot.getContentId(),
                spot.getTitle(),
                spot.getAddr1(),
                spot.getFirstImage(),
                spot.getLikeCount(),
                like.getCreatedAt()
        );
    }

    public static MyLikedSpotItem fromKakao(SpotLike like, KakaoSpot spot, String imageUrl) {
        return new MyLikedSpotItem(
                SpotType.POPULAR,
                spot.getId(),
                spot.getPlaceName(),
                spot.getAddressName(),
                imageUrl,
                spot.getLikeCount(),
                like.getCreatedAt()
        );
    }
}
