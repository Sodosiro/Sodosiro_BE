package com.sodosiro.domain.jwt.exception;

import com.sodosiro.global.payload.code.error.UserErrorCode;
import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

@Getter
public class JwtAuthenticationException extends AuthenticationException {

    private final UserErrorCode userErrorCode;

    public JwtAuthenticationException(UserErrorCode userErrorCode) {
        super(userErrorCode.getMessage());
        this.userErrorCode = userErrorCode;
    }
    public JwtAuthenticationException(UserErrorCode userErrorCode, Throwable e) {
        super(userErrorCode.getMessage(), e);
        this.userErrorCode = userErrorCode;
    }
}
