package com.sodosiro.global.payload.code.error;

import com.sodosiro.global.payload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum BingoErrorCode implements BaseCode {

    _NO_ACTIVE_SEASON(HttpStatus.NOT_FOUND, "BINGO404-NO_ACTIVE_SEASON", "진행 중인 빙고 시즌이 없습니다."),
    _SEASON_NOT_FOUND(HttpStatus.NOT_FOUND, "BINGO404-SEASON_NOT_FOUND", "존재하지 않는 빙고 시즌입니다."),
    _BOARD_NOT_FOUND(HttpStatus.NOT_FOUND, "BINGO404-BOARD_NOT_FOUND", "해당 지역의 빙고판을 찾을 수 없습니다."),
    _INVALID_SEASON_QUERY(HttpStatus.BAD_REQUEST, "BINGO400-INVALID_SEASON_QUERY", "year와 seasonType은 함께 주거나 둘 다 생략해야 합니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
