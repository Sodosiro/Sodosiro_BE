package com.sodosiro.domain.review.service.event;

import java.util.List;

/**
 * newUrls: 방금 S3에 업로드한 URL 목록 — 롤백 시 삭제
 * oldUrls: 교체·삭제된 기존 URL 목록 — 커밋 시 삭제
 */
public record ReviewImageChangedEvent(List<String> newUrls, List<String> oldUrls) {

    public static ReviewImageChangedEvent onlyNew(List<String> newUrls) {
        return new ReviewImageChangedEvent(newUrls, List.of());
    }

    public static ReviewImageChangedEvent onlyOld(List<String> oldUrls) {
        return new ReviewImageChangedEvent(List.of(), oldUrls);
    }

    public static ReviewImageChangedEvent replace(List<String> newUrls, List<String> oldUrls) {
        return new ReviewImageChangedEvent(newUrls, oldUrls);
    }
}
