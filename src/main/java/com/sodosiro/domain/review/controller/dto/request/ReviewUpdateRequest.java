package com.sodosiro.domain.review.controller.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record ReviewUpdateRequest(

        @NotNull(message = "별점은 필수입니다.")
        @DecimalMin(value = "0.1", message = "별점은 0.1점 이상이어야 합니다.")
        @DecimalMax(value = "5.0", message = "별점은 5.0점 이하여야 합니다.")
        @Digits(integer = 1, fraction = 1, message = "별점은 소수점 한 자리까지 입력 가능합니다.")
        @Schema(description = "별점 (0.1~5.0, 소수점 한 자리)", example = "4.5")
        BigDecimal rating,

        @Size(max = 1000, message = "리뷰 내용은 1000자 이하여야 합니다.")
        String body,

        @Schema(description = "유지할 기존 이미지 URL 목록. 여기에 없는 기존 이미지는 삭제됩니다. "
                + "생략/빈 배열이면 기존 이미지를 모두 삭제합니다. 최종 노출 순서는 이 목록 뒤에 새 이미지가 붙습니다.")
        List<String> keepImageUrls
) {
        public List<String> keepImageUrls() {
                return keepImageUrls == null ? List.of() : keepImageUrls;
        }
}
