package com.sodosiro.domain.digging.repository;

import com.sodosiro.domain.digging.entity.DiggingImage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiggingImageRepository extends JpaRepository<DiggingImage, Long> {

    List<DiggingImage> findAllByDiggingIdOrderByDisplayOrderAsc(Long diggingId);

    List<DiggingImage> findAllByDiggingIdInOrderByDiggingIdAscDisplayOrderAsc(Collection<Long> diggingIds);

    void deleteAllByDiggingId(Long diggingId);
}
