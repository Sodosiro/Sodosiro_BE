package com.sodosiro.domain.notification.factory;

import com.sodosiro.domain.notification.command.NotificationCommand;
import com.sodosiro.domain.notification.entity.NotificationType;
import com.sodosiro.domain.notification.trigger.NearbyLikedSpotsDetectedEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class NearbyLikedSpotsNotificationFactory implements NotificationFactory<NearbyLikedSpotsDetectedEvent> {

    @Value("${notification.nearby.cooldown-seconds:21600}")
    private long cooldownSeconds;

    @Override
    public Class<NearbyLikedSpotsDetectedEvent> triggerType() {
        return NearbyLikedSpotsDetectedEvent.class;
    }

    @Override
    public List<NotificationCommand> create(NearbyLikedSpotsDetectedEvent event) {
        String title = event.nearbyCount() == 1
                ? "찜한 장소 %s이(가) 근처에 있어요".formatted(event.nearestSpotTitle())
                : "찜한 장소 %d곳이 근처에 있어요".formatted(event.nearbyCount());

        String body = event.nearbySpotTitles() == null || event.nearbySpotTitles().isEmpty()
                ? event.nearestSpotTitle()
                : String.join(", ", event.nearbySpotTitles());

        return List.of(new NotificationCommand(
                event.userId(),
                NotificationType.NEARBY_LIKED_SPOTS,
                title,
                body,
                Map.of("nearestContentId", event.nearestContentId(), "nearbyCount", event.nearbyCount()),
                "NEARBY:%d".formatted(event.nearestContentId()),
                LocalDateTime.now().plusSeconds(cooldownSeconds)
        ));
    }
}
