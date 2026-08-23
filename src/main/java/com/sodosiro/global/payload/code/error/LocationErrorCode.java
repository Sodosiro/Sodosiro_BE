package com.sodosiro.global.payload.code.error;

import com.sodosiro.global.payload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LocationErrorCode implements BaseCode {

    _LOW_LOCATION_ACCURACY(HttpStatus.BAD_REQUEST, "LOCATION400-LOW_ACCURACY", "위치 정확도가 낮아 처리할 수 없습니다."),
    _STALE_LOCATION_EVENT(HttpStatus.BAD_REQUEST, "LOCATION400-STALE_EVENT", "위치 이벤트가 오래되어 처리할 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
