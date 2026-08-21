package com.sodosiro.global.payload.code.error;

import com.sodosiro.global.payload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum TravelErrorCode implements BaseCode {

    _INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "TRAVEL400-INVALID_PAGE_SIZE", "size는 1~100 사이여야 합니다."),
    _INVALID_CURSOR(HttpStatus.BAD_REQUEST, "TRAVEL400-INVALID_CURSOR", "유효하지 않은 cursor입니다."),
    _SPOT_COORDINATE_MISSING(HttpStatus.BAD_REQUEST, "TRAVEL400-SPOT_COORDINATE_MISSING", "좌표 정보가 없는 여행지는 대체 장소를 조회할 수 없습니다."),
    _TOURIST_SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAVEL404-SPOT_NOT_FOUND", "여행지를 찾을 수 없습니다."),
    _SPOT_EMBEDDING_NOT_FOUND(HttpStatus.NOT_FOUND, "TRAVEL404-EMBEDDING_NOT_FOUND", "여행지 임베딩을 찾을 수 없습니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
