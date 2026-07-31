package com.sodosiro.domain.review.repository;

import com.sodosiro.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long>, ReviewQueryRepository {

    boolean existsByContentIdAndUserIdAndIsDeletedFalse(Long contentId, Long userId);

    Optional<Review> findByIdAndIsDeletedFalse(Long id);
}
