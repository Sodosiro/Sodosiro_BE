package com.sodosiro.domain.like.controller.dto.response;

import java.util.List;

/** 요청한 순서대로 각 관광지의 좋아요 토글 결과를 반환한다. */
public record SpotLikeBatchToggleResponse(List<Item> items) {
    public record Item(Long contentId, boolean liked, int likeCount) { }
}
