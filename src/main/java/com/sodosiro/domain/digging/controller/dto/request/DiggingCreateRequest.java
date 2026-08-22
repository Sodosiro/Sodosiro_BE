package com.sodosiro.domain.digging.controller.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DiggingCreateRequest(

        @NotNull(message = "courseId는 필수입니다.")
        Long courseId,

        @NotNull(message = "contentId는 필수입니다.")
        Long contentId,

        @Size(max = 300, message = "감성 한마디는 최대 300자입니다.")
        String body
) {
}
