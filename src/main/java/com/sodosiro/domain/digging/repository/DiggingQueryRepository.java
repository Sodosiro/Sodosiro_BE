package com.sodosiro.domain.digging.repository;

import com.sodosiro.domain.digging.entity.Digging;
import java.util.List;

public interface DiggingQueryRepository {

    List<Digging> findFeed(Long cursor, int size);

    List<Digging> findByContentId(Long contentId, Long cursor, int size);

    List<Digging> findByUserId(Long userId, Long cursor, int size);
}
