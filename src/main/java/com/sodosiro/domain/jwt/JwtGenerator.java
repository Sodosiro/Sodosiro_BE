package com.sodosiro.domain.jwt;

import com.sodosiro.domain.user.constants.Role;
import com.sodosiro.domain.user.entity.User;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtGenerator {

    private final SecretKey jwtSigningKey;

    private static final String GRANT_TYPE = "Bearer";
    private static final String ROLE_PREFIX = "ROLE_";
    private static final String AUTHORITIES_KEY = "auth";
    private static final String TYPE_KEY = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    @Value("${spring.jwt.issuer}")
    private String jwtIssuer;

    @Value("${spring.jwt.access-token-expiration-millis}")
    private long accessTokenExpirationMillis;

    @Value("${spring.jwt.refresh-token-expiration-millis}")
    private long refreshTokenExpirationMillis;

    public JwtToken generateToken(User user, Role role) {
        long now = System.currentTimeMillis();

        return JwtToken.builder()
                .grantType(GRANT_TYPE)
                .accessToken(buildAccessToken(user, role, now))
                .refreshToken(buildRefreshToken(user, now))
                .build();
    }

    public String createAccessToken(User user, Role role) {
        return buildAccessToken(user, role, System.currentTimeMillis());
    }

    private String buildAccessToken(User user, Role role, long now) {
        return Jwts.builder()
                .issuer(jwtIssuer)
                .subject(user.getUserIdAsString())
                .claim(AUTHORITIES_KEY, ROLE_PREFIX + role.name())
                .claim(TYPE_KEY, TYPE_ACCESS)
                .issuedAt(new Date(now))
                .expiration(new Date(now + accessTokenExpirationMillis))
                .signWith(jwtSigningKey, Jwts.SIG.HS256)
                .compact();
    }

    private String buildRefreshToken(User user, long now) {
        return Jwts.builder()
                .issuer(jwtIssuer)
                .subject(user.getUserIdAsString())
                .claim(TYPE_KEY, TYPE_REFRESH)
                .issuedAt(new Date(now))
                .expiration(new Date(now + refreshTokenExpirationMillis))
                .signWith(jwtSigningKey, Jwts.SIG.HS256)
                .compact();
    }
}
