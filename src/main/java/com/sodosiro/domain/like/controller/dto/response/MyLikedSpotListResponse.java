package com.sodosiro.domain.like.controller.dto.response;

import java.util.List;

public record MyLikedSpotListResponse(
        List<MyLikedSpotItem> content,
        String nextCursor,
        boolean hasNext
) {
}
