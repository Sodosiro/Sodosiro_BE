package com.sodosiro.global.payload.code.error;

import com.sodosiro.global.payload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LikeErrorCode implements BaseCode {

    _TOURIST_SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "LIKE404-TOURIST_NOT_FOUND", "존재하지 않는 관광지입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
