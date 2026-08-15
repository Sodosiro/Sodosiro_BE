package com.sodosiro.domain.like.repository;

import com.sodosiro.domain.like.entity.SpotLike;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SpotLikeRepository extends JpaRepository<SpotLike, Long>, SpotLikeQueryRepository {

    Optional<SpotLike> findByUserIdAndContentId(Long userId, Long contentId);

    List<SpotLike> findByUserIdAndContentIdIn(Long userId, Collection<Long> contentIds);

    @Query("""
            select spotLike.contentId
            from SpotLike spotLike
            where spotLike.userId = :userId
              and spotLike.contentId in :contentIds
            """)
    Set<Long> findLikedContentIdsByUserIdAndContentIds(
            @Param("userId") Long userId,
            @Param("contentIds") Collection<Long> contentIds);
}
