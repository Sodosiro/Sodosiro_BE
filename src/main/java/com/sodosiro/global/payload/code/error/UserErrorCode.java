package com.sodosiro.global.payload.code.error;


import com.sodosiro.global.payload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum UserErrorCode implements BaseCode {

    // User Errors
    _USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER404-NOT_FOUND", "존재하지 않는 회원입니다."),
    _USER_FORBIDDEN(HttpStatus.FORBIDDEN, "USER403-FORBIDDEN", "접근 권한이 없습니다."),
    _DUPLICATE_NICKNAME(HttpStatus.CONFLICT,"USER409-CONFLICT","닉네임이 중복되었습니다."),
    _USER_ALREADY_WITHDRAWN(HttpStatus.CONFLICT,"USER409-ALREADY_WITHDRAWN","이미 탈퇴한 회원입니다."),
    _USER_WITHDRAWN(HttpStatus.UNAUTHORIZED,"USER401-WITHDRAWN","탈퇴한 회원입니다."),
    _PROFILE_IMAGE_NOT_EXIST(HttpStatus.NOT_FOUND,"USER404-PROFILE_NOT_FOUND","프로필 이미지가 존재하지 않습니다."),
    _NICKNAME_BAD_REQUEST(HttpStatus.BAD_REQUEST,"USER400-BAD_REQUEST","닉네임 형식이 올바르지 않습니다."),
    _NICKNAME_GENERATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR,"USER500-NICKNAME_GENERATION_FAILED","기본 닉네임 생성에 실패했습니다."),

    _INVALID_AUTHORIZATION_HEADER(HttpStatus.BAD_REQUEST, "AUTH400-BAD_REQUEST", "유효하지 않은 인증 헤더 형식입니다."),

    // JWT Errors
    _JWT_EXPIRED_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401-EXPIRED_ACCESS", "만료된 엑세스 토큰입니다."),
    _JWT_INVALID_ACCESS_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401-INVALID_ACCESS", "엑세스 토큰이 잘못되었습니다."),
    _JWT_UNSUPPORTED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401-UNSUPPORTED", "지원하지 않는 JWT 토큰입니다."),
    _JWT_INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401-INVALID", "JWT 토큰이 잘못되었습니다."),
    _JWT_INVALID_CLAIMS(HttpStatus.UNAUTHORIZED, "JWT401-INVALID_CLAIMS", "JWT 클레임 정보가 올바르지 않습니다."),
    _JWT_EMPTY_TOKEN(HttpStatus.UNAUTHORIZED,"JWT401-EMPTY_TOKEN", "JWT 토큰이 null이거나 비어있습니다."),
    _JWT_MALFORMED_TOKEN(HttpStatus.UNAUTHORIZED,"JWT401-MALFORMED_TOKEN", "손상된 JWT 토큰입니다."),
    _JWT_BLACKLISTED_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401-BLACKLISTED", "블랙 리스트에 등록 된 토큰입니다. 다시 로그인해주세요."),

    // JWT Refresh Token Errors
    _JWT_EXPIRED_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401-EXPIRED_REFRESH", "만료된 리프레시 토큰입니다."),
    _INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401-INVALID_REFRESH", "리프레시 토큰이 유효하지 않습니다."),
    _INVALID_USER_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "JWT401-INVALID_USER_REFRESH", "리프레시 토큰에 유저ID가 유효하지 않습니다."),

    // Social Login Errors
    _SOCIAL_VERIFICATION_FAILED(HttpStatus.UNAUTHORIZED, "SOCIAL401-VERIFICATION_FAILED", "소셜 토큰 검증 중 오류가 발생했습니다."),
    _SOCIAL_ID_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "IDTOKEN401-INVALID", "소셜 ID 토큰이 유효하지 않습니다."),
    _SOCIAL_ID_TOKEN_MISSING(HttpStatus.BAD_REQUEST, "IDTOKEN400-MISSING", "소셜 ID 토큰이 제공되지 않았습니다."),
    _SOCIAL_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "SOCIAL401-TOKEN_EXPIRED", "소셜 ID 토큰이 만료되었습니다."),
    _SOCIAL_TOKEN_INVALID_SIGNATURE(HttpStatus.UNAUTHORIZED, "SOCIAL401-INVALID_SIGNATURE", "소셜 토큰 서명이 유효하지 않습니다."),
    _SOCIAL_TOKEN_INVALID_AUDIENCE(HttpStatus.UNAUTHORIZED, "SOCIAL401-INVALID_AUDIENCE", "소셜 토큰의 aud 값이 일치하지 않습니다."),
    _SOCIAL_CODE_EXCHANGE_FAILED(HttpStatus.UNAUTHORIZED, "SOCIAL401-CODE_EXCHANGE_FAILED", "소셜 인가코드 교환에 실패했습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
