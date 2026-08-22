package com.sodosiro.global.payload.code.error;

import com.sodosiro.global.payload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum CourseErrorCode implements BaseCode {

    _MUST_VISIT_SPOT_ALWAYS_CLOSED(HttpStatus.BAD_REQUEST, "COURSE400-MUST_VISIT_CLOSED", "필수 방문지가 여행 기간 내내 휴무입니다."),
    _INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "COURSE400-INVALID_DATE_RANGE", "종료일은 시작일보다 빠를 수 없습니다."),
    _REQUIRED_SLOT_INSUFFICIENT(HttpStatus.BAD_REQUEST, "COURSE400-REQUIRED_SLOT_INSUFFICIENT", "필수 슬롯 카테고리에 해당하는 관광지가 부족합니다."),
    _FREE_SLOT_INSUFFICIENT(HttpStatus.BAD_REQUEST, "COURSE400-FREE_SLOT_INSUFFICIENT", "조건에 맞는 관광지가 부족합니다."),
    _COURSE_NOT_STARTABLE(HttpStatus.BAD_REQUEST, "COURSE400-NOT_STARTABLE", "오늘 진행할 수 있는 확정 코스만 시작할 수 있습니다."),
    _TOURIST_SPOT_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE404-SPOT_NOT_FOUND", "여행지를 찾을 수 없습니다."),
    _COURSE_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE404-NOT_FOUND", "코스를 찾을 수 없습니다."),
    _CONTENT_NOT_FOUND(HttpStatus.NOT_FOUND, "COURSE404-CONTENT_NOT_FOUND", "존재하지 않는 관광지가 포함되어 있습니다."),
    _TRANSPORT_MODE_MISMATCH(HttpStatus.BAD_REQUEST, "COURSE400-TRANSPORT_MODE_MISMATCH", "추천 시 선택한 이동수단과 일치하지 않습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
