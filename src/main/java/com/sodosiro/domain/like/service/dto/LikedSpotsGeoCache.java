package com.sodosiro.domain.like.service.dto;


public final class LikedSpotsGeoCache {

    private LikedSpotsGeoCache() {
    }

    public static String redisKey(Long userId) {
        return "user:%d:liked-spots:geo".formatted(userId);
    }
}
