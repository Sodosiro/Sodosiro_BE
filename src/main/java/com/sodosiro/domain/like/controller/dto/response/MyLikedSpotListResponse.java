package com.sodosiro.domain.like.controller.dto.response;

import java.util.List;

public record MyLikedSpotListResponse(
        List<MyLikedSpotItem> content,
        Long nextCursor,
        boolean hasNext
) {
}
