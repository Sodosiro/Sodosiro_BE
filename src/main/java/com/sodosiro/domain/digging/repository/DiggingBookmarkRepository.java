package com.sodosiro.domain.digging.repository;

import com.sodosiro.domain.digging.entity.DiggingBookmark;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiggingBookmarkRepository extends JpaRepository<DiggingBookmark, Long> {

    Optional<DiggingBookmark> findByDiggingIdAndUserId(Long diggingId, Long userId);

    List<DiggingBookmark> findByUserIdAndDiggingIdIn(Long userId, Collection<Long> diggingIds);

    void deleteAllByDiggingId(Long diggingId);
}
