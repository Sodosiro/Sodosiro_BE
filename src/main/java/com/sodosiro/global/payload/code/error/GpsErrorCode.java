package com.sodosiro.global.payload.code.error;

import com.sodosiro.global.payload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum GpsErrorCode implements BaseCode {

    _COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "GPS404-COURSE_NOT_FOUND", "존재하지 않는 코스입니다."),
    _COURSE_SPOT_NOT_FOUND(HttpStatus.BAD_REQUEST, "GPS400-COURSE_SPOT_NOT_FOUND", "해당 코스의 일정에 없는 관광지입니다."),
    _SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "GPS404-SPOT_NOT_FOUND", "존재하지 않는 관광지입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
