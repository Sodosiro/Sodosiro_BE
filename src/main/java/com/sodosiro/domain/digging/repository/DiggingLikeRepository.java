package com.sodosiro.domain.digging.repository;

import com.sodosiro.domain.digging.entity.DiggingLike;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiggingLikeRepository extends JpaRepository<DiggingLike, Long> {

    Optional<DiggingLike> findByDiggingIdAndUserId(Long diggingId, Long userId);

    List<DiggingLike> findByUserIdAndDiggingIdIn(Long userId, Collection<Long> diggingIds);

    void deleteAllByDiggingId(Long diggingId);
}
