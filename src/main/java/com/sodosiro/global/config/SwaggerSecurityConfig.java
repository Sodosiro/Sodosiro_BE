package com.sodosiro.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SwaggerSecurityConfig {

    @Value("${swagger.username}")
    private String username;

    @Value("${swagger.password}")
    private String password;

    @Bean
    @Order(1)
    public SecurityFilterChain swaggerSecurityFilterChain(HttpSecurity http) throws Exception {
        InMemoryUserDetailsManager userDetailsManager = new InMemoryUserDetailsManager(
                User.withUsername(username)
                        .password("{noop}" + password)
                        .roles("SWAGGER")
                        .build()
        );

        http
                .securityMatcher("/swagger-ui/**", "/v3/api-docs/**")
                .userDetailsService(userDetailsManager)
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }
}
