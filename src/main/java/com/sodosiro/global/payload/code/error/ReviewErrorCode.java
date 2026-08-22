package com.sodosiro.global.payload.code.error;

import com.sodosiro.global.payload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReviewErrorCode implements BaseCode {

    _REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW404-NOT_FOUND", "존재하지 않는 리뷰입니다."),
    _REVIEW_FORBIDDEN(HttpStatus.FORBIDDEN, "REVIEW403-FORBIDDEN", "리뷰에 대한 접근 권한이 없습니다."),
    _REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW409-CONFLICT", "이미 해당 관광지에 리뷰를 작성했습니다."),
    _REVIEW_IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "REVIEW400-IMAGE_LIMIT", "이미지는 최대 5개까지 첨부할 수 있습니다."),
    _REVIEW_IMAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "REVIEW400-IMAGE_NOT_FOUND", "해당 리뷰에 속하지 않는 이미지입니다."),
    _SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "SPOT404-NOT_FOUND", "존재하지 않는 관광지입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
