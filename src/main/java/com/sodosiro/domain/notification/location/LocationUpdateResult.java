package com.sodosiro.domain.notification.location;

import java.util.List;

public record LocationUpdateResult(
        boolean processed,
        String ignoredReason,
        int nearbyCount,
        List<String> newlyEnteredContentIds,
        boolean notificationTriggered
) {

    public static LocationUpdateResult ignored(String reason) {
        return new LocationUpdateResult(false, reason, 0, List.of(), false);
    }

    public static LocationUpdateResult processed(int nearbyCount, List<String> newlyEnteredContentIds, boolean notificationTriggered) {
        return new LocationUpdateResult(true, null, nearbyCount, newlyEnteredContentIds, notificationTriggered);
    }
}
