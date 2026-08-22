package com.sodosiro.domain.digging.service.event;

import java.util.List;

/**
 * newUrls: 방금 S3에 업로드한 URL 목록 — 롤백 시 삭제
 * oldUrls: 교체·삭제된 기존 URL 목록 — 커밋 시 삭제
 */
public record DiggingImageChangedEvent(List<String> newUrls, List<String> oldUrls) {

    public static DiggingImageChangedEvent onlyNew(List<String> newUrls) {
        return new DiggingImageChangedEvent(newUrls, List.of());
    }

    public static DiggingImageChangedEvent onlyOld(List<String> oldUrls) {
        return new DiggingImageChangedEvent(List.of(), oldUrls);
    }

    public static DiggingImageChangedEvent replace(List<String> newUrls, List<String> oldUrls) {
        return new DiggingImageChangedEvent(newUrls, oldUrls);
    }
}
