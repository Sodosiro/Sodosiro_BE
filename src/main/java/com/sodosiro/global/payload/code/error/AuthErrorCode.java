package com.sodosiro.global.payload.code.error;

import com.sodosiro.global.payload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum AuthErrorCode implements BaseCode {
    // Auth Errors
    _UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "AUTH400-UNSUPPORTED_PROVIDER", "지원하지 않는 소셜 로그인입니다."),
    _SOCIAL_EMAIL_NOT_PROVIDED(HttpStatus.BAD_REQUEST, "AUTH400-EMAIL_NOT_PROVIDED", "소셜 로그인에서 이메일 정보를 제공받지 못했습니다."),
    _SOCIAL_ID_TOKEN_MISSING(HttpStatus.BAD_REQUEST, "AUTH400-ID_TOKEN_MISSING", "소셜 로그인에서 id_token이 제공되지 않았습니다."),
    _WEB_LOGIN_USER_NOT_FOUND(HttpStatus.NOT_FOUND, "AUTH404-WEB_LOGIN_USER_NOT_FOUND", "가입되지 않은 사용자입니다.")
    ;

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
