package com.example.crypto_divergence.streaming.ws;

import com.example.crypto_divergence.divergence.service.DivergenceDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.Map;

@Component
public class AlertsWebSocketHandler extends TextWebSocketHandler {
    @Autowired
    private DivergenceDetector divergenceDetector;
    @Autowired
    private ObjectMapper objectMapper;

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ScheduledExecutorService broadcastScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "alerts-broadcast");
        thread.setDaemon(true);
        return thread;
    });

    @PostConstruct
    public void startBroadcastLoop() {
        broadcastScheduler.scheduleAtFixedRate(this::broadcastAlerts, 0, 1, TimeUnit.SECONDS);
    }

    @PreDestroy
    public void stop() {
        broadcastScheduler.shutdownNow();
        for (WebSocketSession session : sessions.values()) {
            try { session.close(); } catch (Exception ignored) {}
        }
        sessions.clear();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session.getId());
    }

    private void broadcastAlerts() {
        try {
            String payload = objectMapper.writeValueAsString(divergenceDetector.getAlertHistory());
            for (WebSocketSession session : sessions.values()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {
        }
    }
}
