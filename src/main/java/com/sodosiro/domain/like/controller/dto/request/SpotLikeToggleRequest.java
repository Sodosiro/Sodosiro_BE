package com.sodosiro.domain.like.controller.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** 한 건과 여러 건 좋아요 토글에 공통으로 사용하는 요청 본문. */
public record SpotLikeToggleRequest(
        @NotEmpty(message = "contentIds는 최소 한 개 이상이어야 합니다.")
        @Size(max = 100, message = "한 번에 최대 100개까지 처리할 수 있습니다.")
        List<@NotNull(message = "contentId는 필수입니다.") Long> contentIds
) { }
