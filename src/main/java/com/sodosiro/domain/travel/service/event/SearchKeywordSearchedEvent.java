package com.sodosiro.domain.travel.service.event;

/**
 * @param keyword 검색어 원문
 * @param userId  로그인 사용자 id (비로그인 시 null)
 * @param bot     내부 봇(시드 봇) 요청 여부 — true면 dedup/제한을 무시하고 무조건 집계한다
 */
public record SearchKeywordSearchedEvent(String keyword, Long userId, boolean bot) {
}
