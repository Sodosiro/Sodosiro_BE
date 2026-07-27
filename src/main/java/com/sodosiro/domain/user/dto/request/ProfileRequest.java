package com.sodosiro.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProfileRequest(

        @NotBlank(message = "닉네임은 필수입니다.")
        @Size(min = 2, max = 10, message = "2~10자 사이여야 합니다.")
        String nickName,

        @Size(min = 2, max = 20, message = "2~20자 사이여야 합니다.")
        String introduction
) {
}
