package com.sodosiro.domain.like.repository;

import com.sodosiro.domain.like.entity.SpotLike;

import java.util.List;

public interface SpotLikeQueryRepository {

    List<SpotLike> findByUserId(Long userId, Long cursor, int size);

    List<SpotLike> findByUserIdAndSigunguCode(Long userId, String sigunguCode, Long cursor, int size);
}
