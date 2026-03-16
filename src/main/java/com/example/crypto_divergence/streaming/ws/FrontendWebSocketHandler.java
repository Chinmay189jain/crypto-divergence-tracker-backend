package com.example.crypto_divergence.streaming.ws;

import com.example.crypto_divergence.market.service.MarketStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Pushes the latest market data to all connected frontend clients every second.
 */
@Component
@RequiredArgsConstructor
public class FrontendWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(FrontendWebSocketHandler.class);

    private static final int SEND_TIME_LIMIT_MS = 5_000;
    private static final int BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    private final MarketStateService marketStateService;
    private final ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService broadcastScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "frontend-market-broadcast");
                thread.setDaemon(true);
                return thread;
            });

    @PostConstruct
    public void startBroadcastLoop() {
        broadcastScheduler.scheduleAtFixedRate(this::broadcastLatest, 0, 1, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        broadcastScheduler.shutdownNow();

        for (WebSocketSession session : sessions.values()) {
            closeQuietly(session);
        }
        sessions.clear();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketSession safeSession = new ConcurrentWebSocketSessionDecorator(
                session,
                SEND_TIME_LIMIT_MS,
                BUFFER_SIZE_LIMIT_BYTES
        );

        sessions.put(safeSession.getId(), safeSession);
        log.info("Frontend WS connected: {}", safeSession.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
        log.info("Frontend WS disconnected: {}, status={}", session.getId(), status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Frontend WS transport error for session {}", session.getId(), exception);
        sessions.remove(session.getId());
        closeQuietly(session);
    }

    private void broadcastLatest() {
        if (sessions.isEmpty()) {
            return;
        }

        try {
            String payload = objectMapper.writeValueAsString(marketStateService.getAllLatest());
            TextMessage message = new TextMessage(payload);

            for (WebSocketSession session : sessions.values()) {
                if (!session.isOpen()) {
                    sessions.remove(session.getId());
                    continue;
                }

                try {
                    session.sendMessage(message);
                } catch (IOException | IllegalStateException e) {
                    log.warn("Failed to send update to session {}", session.getId(), e);
                    sessions.remove(session.getId());
                    closeQuietly(session);
                }
            }
        } catch (Exception e) {
            log.error("Failed to broadcast latest market data", e);
        }
    }

    private void closeQuietly(WebSocketSession session) {
        try {
            if (session.isOpen()) {
                session.close();
            }
        } catch (IOException e) {
            log.debug("Error while closing session {}", session.getId(), e);
        }
    }
}