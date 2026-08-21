package com.sodosiro.global.payload.code.error;

import com.sodosiro.global.payload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DiggingErrorCode implements BaseCode {

    _COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "DIGGING404-COURSE_NOT_FOUND", "존재하지 않는 코스입니다."),
    _COURSE_NOT_FINISHED(HttpStatus.CONFLICT, "DIGGING409-COURSE_NOT_FINISHED", "아직 완료되지 않은 코스입니다."),
    _COURSE_SPOT_NOT_FOUND(HttpStatus.BAD_REQUEST, "DIGGING400-COURSE_SPOT_NOT_FOUND", "해당 코스에 포함되지 않은 관광지입니다."),
    _DIGGING_ALREADY_EXISTS(HttpStatus.CONFLICT, "DIGGING409-ALREADY_EXISTS", "해당 코스의 관광지에 이미 디깅을 작성했습니다."),
    _DIGGING_NOT_FOUND(HttpStatus.NOT_FOUND, "DIGGING404-NOT_FOUND", "존재하지 않는 디깅입니다."),
    _DIGGING_FORBIDDEN(HttpStatus.FORBIDDEN, "DIGGING403-FORBIDDEN", "본인이 작성한 디깅이 아닙니다."),
    _IMAGE_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "DIGGING400-IMAGE_LIMIT", "이미지는 최대 5장까지 첨부할 수 있습니다."),
    _IMAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "DIGGING400-IMAGE_NOT_FOUND", "유지하려는 이미지가 존재하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
