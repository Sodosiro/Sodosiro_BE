package com.sodosiro.domain.notification.location;

import com.sodosiro.domain.jwt.JwtProvider;
import com.sodosiro.global.service.RedisService;
import com.sodosiro.global.utils.TokenKeys;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

@Component
@RequiredArgsConstructor
public class LocationHandshakeInterceptor implements HandshakeInterceptor {
    private final JwtProvider jwtProvider;
    private final RedisService redisService;

    @Override
    public boolean beforeHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler webSocketHandler,
            @NonNull Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }
        HttpServletRequest httpRequest = servletRequest.getServletRequest();
        String token = jwtProvider.resolveToken(httpRequest);

        if (token == null || redisService.hasKey(TokenKeys.blacklistKey(token))) {
            return false;
        }

        try {
            Authentication authentication = jwtProvider.getAuthentication(token);
            UserDetails principal = (UserDetails) authentication.getPrincipal();
            attributes.put("userId", Long.parseLong(Objects.requireNonNull(principal).getUsername()));
            return true;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    @Override
    public void afterHandshake(
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response,
            @NonNull WebSocketHandler webSocketHandler,
            Exception exception) {
    }
}
