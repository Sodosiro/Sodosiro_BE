package com.sodosiro.domain.digging.controller.dto.response;

import java.util.List;

public record DiggingListResponse(
        List<DiggingResponse> items,
        Long nextCursor,
        boolean hasNext
) {
}
