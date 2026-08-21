package com.sodosiro.domain.notification.trigger;

import java.util.List;

public record NearbyLikedSpotsDetectedEvent(
        Long userId,
        Long nearestContentId,
        String nearestSpotTitle,
        int nearbyCount,
        List<String> nearbySpotTitles
) implements NotificationTrigger {
}
