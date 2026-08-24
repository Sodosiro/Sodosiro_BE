package com.sodosiro.domain.notification.controller.dto;

import com.sodosiro.domain.notification.entity.NotificationPreference;

public record NotificationPreferenceResponse(
        boolean allEnabled,
        boolean nearbyLikedSpotsEnabled,
        boolean reviewRequestEnabled,
        boolean diggingPostLikeEnabled
) {
    public static NotificationPreferenceResponse from(NotificationPreference preference) {
        return new NotificationPreferenceResponse(
                preference.isAllEnabled(),
                preference.isNearbyLikedSpotsEnabled(),
                preference.isReviewRequestEnabled(),
                preference.isDiggingPostLikeEnabled());
    }
}
