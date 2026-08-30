package com.sodosiro.domain.review.repository;

import com.sodosiro.domain.review.entity.ReviewImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ReviewImageRepository extends JpaRepository<ReviewImage, Long>, ReviewImageQueryRepository {

    List<ReviewImage> findAllByReviewIdOrderByDisplayOrderAsc(Long reviewId);

    List<ReviewImage> findAllByReviewIdInOrderByReviewIdAscDisplayOrderAsc(List<Long> reviewIds);

    void deleteAllByReviewIdIn(Collection<Long> reviewIds);
}
