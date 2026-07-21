package com.sodosiro.domain.jwt;


import com.sodosiro.domain.jwt.exception.JwtAuthenticationException;
import com.sodosiro.global.payload.code.error.UserErrorCode;
import com.sodosiro.global.payload.exception.GeneralException;
import io.jsonwebtoken.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private static final String BEARER = "Bearer ";
    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private final JwtParser parser;

    public Authentication getAuthentication(String accessToken) {

        Claims claims = parseClaims(accessToken);

        String subject = claims.getSubject();
        if (!StringUtils.hasText(subject)) {
            throw new JwtAuthenticationException(UserErrorCode._JWT_INVALID_CLAIMS);
        }

        Object auth = claims.get("auth");
        if (!(auth instanceof String authStr) || !StringUtils.hasText(authStr)) {
            throw new JwtAuthenticationException(UserErrorCode._JWT_INVALID_CLAIMS);
        }

        Object type = claims.get("type");
        if (!"access".equals(type)) {
            throw new JwtAuthenticationException(UserErrorCode._JWT_INVALID_CLAIMS);
        }

        Collection<? extends GrantedAuthority> authorities = Arrays.stream(authStr.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(SimpleGrantedAuthority::new)
                .toList();

        if (authorities.isEmpty()) {
            throw new JwtAuthenticationException(UserErrorCode._JWT_INVALID_CLAIMS);
        }

        UserDetails principal = new User(claims.getSubject(), "", authorities);
        return new UsernamePasswordAuthenticationToken(principal, "", authorities);
    }

    private Claims parseClaims(String token) {
        try {
            return parser.parseSignedClaims(token).getPayload();
        } catch (ExpiredJwtException e) {
            throw new JwtAuthenticationException(UserErrorCode._JWT_EXPIRED_ACCESS_TOKEN, e);
        } catch (UnsupportedJwtException e) {
            throw new JwtAuthenticationException(UserErrorCode._JWT_UNSUPPORTED_TOKEN, e);
        } catch (IllegalArgumentException e) {
            throw new JwtAuthenticationException(UserErrorCode._JWT_EMPTY_TOKEN, e);
        } catch (MalformedJwtException e) {
            throw new JwtAuthenticationException(UserErrorCode._JWT_MALFORMED_TOKEN, e);
        } catch (JwtException e) {
            throw new JwtAuthenticationException(UserErrorCode._JWT_INVALID_TOKEN, e);
        }
    }

    public void validateRefreshToken(String refreshToken) {
        try {
            Claims claims = parser.parseSignedClaims(refreshToken).getPayload();
            if (!"refresh".equals(claims.get("type"))) {
                throw new GeneralException(UserErrorCode._JWT_INVALID_TOKEN);
            }
        } catch (ExpiredJwtException e) {
            throw new GeneralException(UserErrorCode._JWT_EXPIRED_REFRESH_TOKEN, e);
        } catch (UnsupportedJwtException e) {
            throw new GeneralException(UserErrorCode._JWT_UNSUPPORTED_TOKEN, e);
        } catch (IllegalArgumentException e) {
            throw new GeneralException(UserErrorCode._JWT_EMPTY_TOKEN, e);
        } catch (MalformedJwtException e) {
            throw new GeneralException(UserErrorCode._JWT_MALFORMED_TOKEN, e);
        } catch (JwtException e) {
            throw new GeneralException(UserErrorCode._INVALID_REFRESH_TOKEN, e);
        }
    }

    public String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader("Authorization");
        if (StringUtils.hasText(bearer) && bearer.startsWith(BEARER)) {
            return bearer.substring(BEARER.length());
        }

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies)
                .filter(c -> ACCESS_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }


    public long getExpiration(String accessToken) {
        try {
            Claims claims = parser.parseSignedClaims(accessToken).getPayload();
            return Math.max(0, claims.getExpiration().getTime() - System.currentTimeMillis());
        } catch (ExpiredJwtException e) {
            return 0;
        } catch (Exception e) {
            throw new GeneralException(UserErrorCode._JWT_INVALID_ACCESS_TOKEN, e);
        }
    }
}


