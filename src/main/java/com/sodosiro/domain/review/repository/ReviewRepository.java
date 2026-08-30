package com.sodosiro.domain.review.repository;

import com.sodosiro.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewQueryRepository {

    boolean existsByContentIdAndUserIdAndIsDeletedFalse(Long contentId, Long userId);

    Optional<Review> findByIdAndIsDeletedFalse(Long id);

    Optional<Review> findByContentIdAndUserIdAndIsDeletedFalse(Long contentId, Long userId);

    List<Review> findByUserIdAndContentIdInAndIsDeletedFalse(Long userId, Collection<Long> contentIds);

    List<Review> findTop5ByContentIdAndIsDeletedFalseOrderByCreatedAtDesc(Long contentId);

    List<Review> findTop3ByContentIdAndIsDeletedFalseOrderByCreatedAtDesc(Long contentId);

    List<Review> findAllByUserId(Long userId);

    void deleteAllByUserId(Long userId);
}
