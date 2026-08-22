package com.sodosiro.domain.review.repository;

/**
 * "이 사용자가 이 관광지에 리뷰를 썼다"는 사실 하나를 나타내는 키. (멱등성임)
 **/
public record ReviewKey(Long userId, Long contentId) {
}
