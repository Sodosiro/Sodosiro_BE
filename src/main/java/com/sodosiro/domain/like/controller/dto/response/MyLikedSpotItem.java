package com.sodosiro.domain.like.controller.dto.response;

import com.sodosiro.domain.like.entity.SpotLike;
import com.sodosiro.domain.travel.entity.TouristSpot;

import java.time.LocalDateTime;

public record MyLikedSpotItem(
        Long contentId,
        String title,
        Integer category,
        String addr1,
        String overview,
        String firstImage,
        Integer likeCount,
        LocalDateTime likedAt
) {
    public static MyLikedSpotItem from(SpotLike like, TouristSpot spot) {
        return new MyLikedSpotItem(
                spot.getContentId(),
                spot.getTitle(),
                spot.getCategory(),
                spot.getAddr1(),
                spot.getOverview(),
                spot.getFirstImage(),
                spot.getLikeCount(),
                like.getCreatedAt()
        );
    }
}
