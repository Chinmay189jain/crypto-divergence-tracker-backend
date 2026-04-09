package com.example.crypto_divergence.config;

import com.example.crypto_divergence.streaming.ws.FrontendWebSocketHandler;
import com.example.crypto_divergence.streaming.ws.AlertsWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    private final FrontendWebSocketHandler frontendWebSocketHandler;
    private final AlertsWebSocketHandler alertsWebSocketHandler;

    public WebSocketConfig(FrontendWebSocketHandler frontendWebSocketHandler, AlertsWebSocketHandler alertsWebSocketHandler) {
        this.frontendWebSocketHandler = frontendWebSocketHandler;
        this.alertsWebSocketHandler = alertsWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(frontendWebSocketHandler, "/ws/market").setAllowedOrigins("*");
        registry.addHandler(alertsWebSocketHandler, "/ws/alerts").setAllowedOrigins("*");
    }
}
