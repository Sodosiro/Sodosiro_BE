package com.sodosiro.domain.notification.location.controller.dto;

import com.sodosiro.domain.notification.location.LocationUpdateResult;
import java.util.List;

public record LocationUpdateResponse(
        boolean processed,
        String ignoredReason,
        int nearbyCount,
        List<String> newlyEnteredContentIds,
        boolean notificationTriggered
) {
    public static LocationUpdateResponse from(LocationUpdateResult result) {
        return new LocationUpdateResponse(
                result.processed(),
                result.ignoredReason(),
                result.nearbyCount(),
                result.newlyEnteredContentIds(),
                result.notificationTriggered());
    }
}
