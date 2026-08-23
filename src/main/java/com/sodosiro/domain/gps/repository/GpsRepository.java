package com.sodosiro.domain.gps.repository;

import com.sodosiro.domain.gps.entity.Gps;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GpsRepository extends JpaRepository<Gps, Long> {

    Optional<Gps> findByCourseIdAndContentIdAndDay(Long courseId, Long contentId, Integer day);

    List<Gps> findByCourseId(Long courseId);

    List<Gps> findByCourseIdIn(List<Long> courseIds);

    boolean existsByUserIdAndContentId(Long userId, Long contentId);

    boolean existsByCourseIdAndContentId(Long courseId, Long contentId);
}
