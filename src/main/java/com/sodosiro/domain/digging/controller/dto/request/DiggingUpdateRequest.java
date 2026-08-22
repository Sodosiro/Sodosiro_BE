package com.sodosiro.domain.digging.controller.dto.request;

import jakarta.validation.constraints.Size;
import java.util.List;

public record DiggingUpdateRequest(

        @Size(max = 300, message = "감성 한마디는 최대 300자입니다.")
        String body,

        List<String> keepImageUrls
) {
    public List<String> keepImageUrls() {
        return keepImageUrls == null ? List.of() : keepImageUrls;
    }
}
