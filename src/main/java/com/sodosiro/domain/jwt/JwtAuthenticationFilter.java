package com.sodosiro.domain.jwt;

import com.sodosiro.domain.jwt.exception.JwtAuthenticationException;
import com.sodosiro.global.payload.code.error.UserErrorCode;
import com.sodosiro.global.service.RedisService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RedisService redisService;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = jwtProvider.resolveToken(request);

        if (StringUtils.hasText(token)) {
            try {
                if (redisService.hasKey("BLACKLIST:" + token)) {
                    throw new JwtAuthenticationException(UserErrorCode._JWT_BLACKLISTED_TOKEN);
                }
                Authentication authentication = jwtProvider.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtAuthenticationException e) {
                SecurityContextHolder.clearContext();
                log.warn("JWT 인증 실패 [{}] {} - {}", request.getMethod(), request.getRequestURI(), e.getMessage());
                request.setAttribute("exception", e);
            }
        }

        filterChain.doFilter(request, response);
    }

}