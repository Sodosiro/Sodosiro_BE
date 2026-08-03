package com.sodosiro.domain.travel.repository;

import com.sodosiro.domain.travel.entity.SpotAiRecommendation;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotAiRecommendationRepository extends JpaRepository<SpotAiRecommendation, Long> {
    Optional<SpotAiRecommendation> findByContentIdAndExpiresAtAfter(Long contentId, LocalDateTime now);
}
