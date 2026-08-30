package com.sodosiro.domain.review.service;

import com.sodosiro.domain.review.repository.ReviewRepository;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class SpotRatingStatsUpdater {

    private final ReviewRepository reviewRepository;
    private final TouristSpotRepository touristSpotRepository;

    public void refresh(Long contentId) {
        Double average = reviewRepository.avgRatingByContentId(contentId);
        long count = reviewRepository.countActiveByContentId(contentId);
        BigDecimal rounded = average == null
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
        touristSpotRepository.updateRatingStats(contentId, rounded, (int) count);
    }

    public void refreshAll(Collection<Long> contentIds) {
        contentIds.forEach(this::refresh);
    }
}
