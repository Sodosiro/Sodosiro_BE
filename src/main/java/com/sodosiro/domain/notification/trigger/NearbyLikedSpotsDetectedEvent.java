package com.sodosiro.domain.notification.trigger;

import java.util.List;

public record NearbyLikedSpotsDetectedEvent(
        Long userId,
        Long courseId,
        java.time.LocalDate courseEndDate,
        Long nearestContentId,
        String nearestSpotTitle,
        int nearbyCount,
        List<String> nearbySpotTitles,
        List<Long> nearbyContentIds
) implements NotificationTrigger {
}
