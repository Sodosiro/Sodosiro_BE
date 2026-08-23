package com.sodosiro.global.config;

import com.sodosiro.domain.jwt.JwtAuthenticationFilter;
import com.sodosiro.domain.jwt.JwtProvider;
import com.sodosiro.global.payload.exception.handler.JwtAccessDeniedHandler;
import com.sodosiro.global.payload.exception.handler.JwtAuthenticationEntryPoint;
import com.sodosiro.global.service.RedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

/**
 * 초기 세팅용 시큐리티 골격 설정.
 * <p>
 * 현재는 모든 요청을 허용(permitAll)하고, 세션은 STATELESS 로 둔다.
 * 도메인/인증(JWT·Kakao OAuth2)이 붙으면서 인가 규칙·필터·핸들러를 아래 TODO 위치에 채운다.
 */
@EnableWebSecurity
@RequiredArgsConstructor
@Configuration
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtProvider jwtProvider;
    private final RedisService redisService;


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
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        // /api/v1/regions/** 는 아래에서 permitAll 이지만, 본인 GPS 기록을 보여주는
                        // /visited 만은 로그인이 필요하다 — 더 구체적인 규칙이라 wildcard 보다 먼저 선언한다.
                        .requestMatchers("/api/v1/regions/visited").authenticated()
                        .requestMatchers("/", "/login/**", "/oauth2/**", "/error",
                                "/v1/auth/reissue", "/v1/auth/social", "/v1/travel/**",
                                "/api/v1/travel/**", "/api/v1/regions/**", "/api/v1/festivals/**",
                                "/internal/etl/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint)
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);


        // TODO: JWT 인증 필터 등록 (addFilterBefore(jwtAuthenticationFilter, ...))
        // TODO: OAuth2 로그인(Kakao) 설정 (oauth2Login)
        // TODO: 예외 핸들러 등록 (authenticationEntryPoint / accessDeniedHandler)

        return http.build();
    }
    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, redisService);
    }

}
