package com.sodosiro.domain.digging.repository;

import com.sodosiro.domain.digging.entity.Digging;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiggingRepository extends JpaRepository<Digging, Long>, DiggingQueryRepository {

    Optional<Digging> findByIdAndIsDeletedFalse(Long id);

    boolean existsByCourseIdAndContentIdAndIsDeletedFalse(Long courseId, Long contentId);

    List<Digging> findByCourseIdAndIsDeletedFalse(Long courseId);
}
