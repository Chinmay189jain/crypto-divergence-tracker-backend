package com.example.crypto_divergence.config;

import com.example.crypto_divergence.streaming.ws.FrontendWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Configures the WebSocket endpoint for frontend clients to receive real-time market data.
 * Exposes /ws/market for WebSocket connections.
 */
@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {
    private final FrontendWebSocketHandler frontendWebSocketHandler;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // TODO: For production, restrict allowed origins instead of using "*"
        registry.addHandler(frontendWebSocketHandler, "/ws/market").setAllowedOrigins("*");
    }
}
