package com.sodosiro.domain.travel.repository;

public interface KakaoSpotQueryRepository {

    void incrementLikeCount(Long kakaoSpotId);

    void decrementLikeCount(Long kakaoSpotId);
}
