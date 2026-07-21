package com.sodosiro.global.payload.exception.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sodosiro.domain.jwt.exception.JwtAuthenticationException;
import com.sodosiro.global.payload.code.ReasonDTO;
import com.sodosiro.global.payload.code.error.UserErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {

        UserErrorCode errorCode;

        Object stored = request.getAttribute("exception");
        if (stored instanceof JwtAuthenticationException jwtEx) {
            errorCode = jwtEx.getUserErrorCode();
        } else {
            errorCode = UserErrorCode._JWT_EMPTY_TOKEN;
        }

        response.setStatus(errorCode.getHttpStatus().value());
        response.setContentType("application/json;charset=UTF-8");

        ReasonDTO errorResponse = errorCode.getReasonHttpStatus();

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
