package com.sodosiro.global.config;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.crypto.SecretKey;

@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtSigningKey(@Value("${spring.jwt.secret}") String secret) {
        byte[] decoded = Decoders.BASE64.decode(secret);

        if (decoded.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes long, but was " + decoded.length);
        }

        return Keys.hmacShaKeyFor(decoded);
    }

    @Bean
    public JwtParser jwtParser(SecretKey jwtSigningKey, @Value("${spring.jwt.issuer}") String issuer) {
        return Jwts.parser()
                .verifyWith(jwtSigningKey)
                .requireIssuer(issuer)
                .clockSkewSeconds(30)
                .build();
    }

}

