package com.sodosiro.domain.review.service.event;

import java.util.List;

/**
 * newUrls: 방금 S3에 업로드한 URL 목록 — 롤백 시 삭제
 * oldUrls: 교체·삭제된 기존 URL 목록 — 커밋 시 삭제
 */
public record ReviewImageChangedEvent(List<String> newUrls, List<String> oldUrls) {

    /**
     * Creates an event containing newly uploaded image URLs.
     *
     * @param newUrls the URLs of newly uploaded images
     * @return an event with the specified new URLs and no old URLs
     */
    public static ReviewImageChangedEvent onlyNew(List<String> newUrls) {
        return new ReviewImageChangedEvent(newUrls, List.of());
    }

    /**
     * Creates an event containing URLs of images that were replaced or removed.
     *
     * @param oldUrls URLs to delete after the associated change is committed
     * @return an event containing only the specified old URLs
     */
    public static ReviewImageChangedEvent onlyOld(List<String> oldUrls) {
        return new ReviewImageChangedEvent(List.of(), oldUrls);
    }

    /**
     * Creates an event containing newly uploaded image URLs and replaced or removed image URLs.
     *
     * @param newUrls URLs newly uploaded to storage
     * @param oldUrls URLs replaced or removed from storage
     * @return an event containing both URL lists
     */
    public static ReviewImageChangedEvent replace(List<String> newUrls, List<String> oldUrls) {
        return new ReviewImageChangedEvent(newUrls, oldUrls);
    }
}
