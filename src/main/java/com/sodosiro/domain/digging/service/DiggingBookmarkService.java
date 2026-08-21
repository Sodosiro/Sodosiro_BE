package com.sodosiro.domain.digging.service;

import com.sodosiro.domain.digging.controller.dto.response.DiggingBookmarkResponse;
import com.sodosiro.domain.digging.entity.DiggingBookmark;
import com.sodosiro.domain.digging.repository.DiggingBookmarkRepository;
import com.sodosiro.domain.digging.repository.DiggingRepository;
import com.sodosiro.global.payload.code.error.DiggingErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DiggingBookmarkService {

    private final DiggingRepository diggingRepository;
    private final DiggingBookmarkRepository diggingBookmarkRepository;

    @Transactional
    public DiggingBookmarkResponse toggle(Long userId, Long diggingId) {
        validateExists(diggingId);

        Optional<DiggingBookmark> existing = diggingBookmarkRepository.findByDiggingIdAndUserId(diggingId, userId);
        if (existing.isPresent()) {
            diggingBookmarkRepository.delete(existing.get());
            return new DiggingBookmarkResponse(false);
        }

        diggingBookmarkRepository.save(DiggingBookmark.of(diggingId, userId));
        return new DiggingBookmarkResponse(true);
    }

    private void validateExists(Long diggingId) {
        if (diggingRepository.findByIdAndIsDeletedFalse(diggingId).isEmpty()) {
            throw new GeneralException(DiggingErrorCode._DIGGING_NOT_FOUND);
        }
    }
}
