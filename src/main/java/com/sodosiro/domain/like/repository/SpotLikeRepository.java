package com.sodosiro.domain.like.repository;

import com.sodosiro.domain.like.entity.SpotLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpotLikeRepository extends JpaRepository<SpotLike, Long>, SpotLikeQueryRepository {

    Optional<SpotLike> findByUserIdAndContentId(Long userId, Long contentId);
}
