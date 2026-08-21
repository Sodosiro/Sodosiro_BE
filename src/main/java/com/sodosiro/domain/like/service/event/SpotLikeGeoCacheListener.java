package com.sodosiro.domain.like.service.event;

import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component @RequiredArgsConstructor @Slf4j
public class SpotLikeGeoCacheListener {
    private final RedisService redisService;
    private final TouristSpotRepository touristSpotRepository;
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void sync(SpotLikeChangedEvent event) {
        String key = "user:%d:liked-spots:geo".formatted(event.userId());
        if (!event.liked()) { redisService.removeFromGeo(key, String.valueOf(event.contentId())); return; }
        touristSpotRepository.findById(event.contentId()).ifPresent(spot -> {
            if (spot.getMapX() == null || spot.getMapY() == null) { log.warn("좋아요 GEO 캐시를 갱신하지 못했습니다. 좌표 없음: contentId={}", event.contentId()); return; }
            redisService.addGeo(key, spot.getMapX().doubleValue(), spot.getMapY().doubleValue(), String.valueOf(event.contentId()));
        });
    }
}
