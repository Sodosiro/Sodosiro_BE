package com.sodosiro.domain.notification.location;

import com.sodosiro.domain.notification.facade.NotificationFacade;
import com.sodosiro.domain.notification.factory.NearbyLikedSpotsNotificationFactory;
import com.sodosiro.domain.notification.trigger.NearbyLikedSpotsDetectedEvent;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LocationNotificationService {

    private final NearbyLikedSpotsNotificationFactory notificationFactory;
    private final NotificationFacade notificationFacade;

    @Transactional
    public void createNearbyNotification(
            Long userId,
            Long nearestContentId,
            String nearestSpotTitle,
            int nearbyCount,
            List<String> nearbySpotTitles) {
        NearbyLikedSpotsDetectedEvent event = new NearbyLikedSpotsDetectedEvent(
                userId,
                nearestContentId,
                nearestSpotTitle,
                nearbyCount,
                nearbySpotTitles
        );
        notificationFactory.create(event).forEach(notificationFacade::create);
    }
}
