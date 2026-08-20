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
    _SPOT_LOCATION_UNAVAILABLE(HttpStatus.CONFLICT, "GPS409-SPOT_LOCATION", "관광지 위치 정보가 없어 GPS 인증을 할 수 없습니다."),
    _SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "GPS404-SPOT_NOT_FOUND", "존재하지 않는 관광지입니다."),
    _OUT_OF_VERIFICATION_RANGE(HttpStatus.CONFLICT, "GPS409-OUT_OF_RANGE", "관광지 반경 300m 밖에서는 GPS 인증을 할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
