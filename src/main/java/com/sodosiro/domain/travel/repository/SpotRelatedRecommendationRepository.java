package com.sodosiro.domain.travel.repository;

import com.sodosiro.domain.travel.entity.SpotRelatedRecommendation;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotRelatedRecommendationRepository extends JpaRepository<SpotRelatedRecommendation, Long> {
    Optional<SpotRelatedRecommendation> findByContentIdAndExpiresAtAfter(Long contentId, LocalDateTime now);
}
