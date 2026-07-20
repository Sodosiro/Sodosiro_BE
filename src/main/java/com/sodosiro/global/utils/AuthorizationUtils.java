package com.sodosiro.global.utils;

import com.sodosiro.global.payload.code.error.UserErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;

public class AuthorizationUtils {
    public static String extractBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new GeneralException(UserErrorCode._INVALID_AUTHORIZATION_HEADER);
        }
        return authorizationHeader.substring(7);
    }
}