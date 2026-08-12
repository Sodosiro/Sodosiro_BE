package com.sodosiro.global.payload.code.error;

import com.sodosiro.global.payload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CourseErrorCode implements BaseCode {

    _MUST_VISIT_SPOT_ALWAYS_CLOSED(HttpStatus.BAD_REQUEST, "COURSE400-MUST_VISIT_CLOSED", "필수 방문지가 여행 기간 내내 휴무입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
