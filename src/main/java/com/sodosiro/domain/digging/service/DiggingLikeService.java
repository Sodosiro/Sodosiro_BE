package com.sodosiro.domain.digging.service;

import com.sodosiro.domain.digging.controller.dto.response.DiggingLikeResponse;
import com.sodosiro.domain.digging.entity.Digging;
import com.sodosiro.domain.digging.entity.DiggingLike;
import com.sodosiro.domain.digging.repository.DiggingLikeRepository;
import com.sodosiro.domain.digging.repository.DiggingRepository;
import com.sodosiro.domain.notification.trigger.DiggingLikedEvent;
import com.sodosiro.domain.travel.entity.TouristSpot;
import com.sodosiro.domain.travel.repository.TouristSpotRepository;
import com.sodosiro.global.payload.code.error.DiggingErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiggingLikeService {

    private final DiggingRepository diggingRepository;
    private final DiggingLikeRepository diggingLikeRepository;
    private final TouristSpotRepository touristSpotRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public DiggingLikeResponse toggle(Long userId, Long diggingId) {
        Digging digging = findDigging(diggingId);

        Optional<DiggingLike> existing = diggingLikeRepository.findByDiggingIdAndUserId(diggingId, userId);
        if (existing.isPresent()) {
            diggingLikeRepository.delete(existing.get());
            digging.decreaseLikeCount();
            return new DiggingLikeResponse(false, digging.getLikeCount());
        }

        diggingLikeRepository.save(DiggingLike.of(diggingId, userId));
        digging.increaseLikeCount();

        noSendNotificationMyDigging(userId, diggingId, digging);
        return new DiggingLikeResponse(true, digging.getLikeCount());
    }

    private void noSendNotificationMyDigging(Long userId, Long diggingId, Digging digging) {
        if (!digging.getUserId().equals(userId)) {
            eventPublisher.publishEvent(new DiggingLikedEvent(
                    digging.getUserId(),
                    userId,
                    diggingId,
                    touristSpotRepository.findById(digging.getContentId())
                            .map(TouristSpot::getTitle).orElse(null),
                    digging.getLikeCount()
            ));
        }
    }

    private Digging findDigging(Long diggingId) {
        return diggingRepository.findByIdAndIsDeletedFalse(diggingId)
                .orElseThrow(() -> new GeneralException(DiggingErrorCode._DIGGING_NOT_FOUND));
    }
}
