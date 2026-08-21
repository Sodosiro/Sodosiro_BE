package com.sodosiro.domain.notification.location;

import java.time.Instant;

public record LocationUpdateRequest(
        String type,
        Long courseId,
        Double latitude,
        Double longitude,
        Double accuracy,
        Instant occurredAt
) {
}
