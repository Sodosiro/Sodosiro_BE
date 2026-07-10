package com.sodosiro.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * 초기 세팅용 시큐리티 골격 설정.
 * <p>
 * 현재는 모든 요청을 허용(permitAll)하고, 세션은 STATELESS 로 둔다.
 * 도메인/인증(JWT·Kakao OAuth2)이 붙으면서 인가 규칙·필터·핸들러를 아래 TODO 위치에 채운다.
 */
@Configuration
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll()
                );

        // TODO: JWT 인증 필터 등록 (addFilterBefore(jwtAuthenticationFilter, ...))
        // TODO: OAuth2 로그인(Kakao) 설정 (oauth2Login)
        // TODO: 예외 핸들러 등록 (authenticationEntryPoint / accessDeniedHandler)

        return http.build();
    }
}
