package com.sodosiro.domain.like.service.event;
public record SpotLikeChangedEvent(Long userId, Long contentId, boolean liked) { }
