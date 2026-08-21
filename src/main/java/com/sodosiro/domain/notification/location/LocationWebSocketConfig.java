package com.sodosiro.domain.notification.location;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class LocationWebSocketConfig implements WebSocketConfigurer {
    private final LocationWebSocketHandler locationWebSocketHandler;
    private final LocationHandshakeInterceptor locationHandshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(locationWebSocketHandler, "/api/v1/ws/locations")
                .addInterceptors(locationHandshakeInterceptor)
                .setAllowedOrigins("https://sodosiro.store", "http://localhost:3000", "http://localhost:8081");
    }
}
