package com.example.crypto_divergence.market.wsclient;

import com.example.crypto_divergence.market.constants.SymbolConstants;
import com.example.crypto_divergence.market.dto.BinanceCombinedStreamMessage;
import com.example.crypto_divergence.market.dto.BinanceTickerData;
import com.example.crypto_divergence.market.model.MarketTick;
import com.example.crypto_divergence.market.service.MarketStateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Connects to Binance WebSocket API, receives ticker data,
 * and updates the in-memory market state.
 */
@Component
@RequiredArgsConstructor
class BinanceWsClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceWsClient.class);

    private final MarketStateService marketStateService;
    private final ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final ScheduledExecutorService reconnectScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "binance-ws-reconnect");
                thread.setDaemon(true);
                return thread;
            });

    private final AtomicBoolean reconnectScheduled = new AtomicBoolean(false);

    private volatile WebSocket webSocket;
    private volatile boolean running = false;

    @Value("${binance.ws.base-url}")
    private String binanceBaseUrl;
    @Value("${binance.ws.symbols}")
    private String binanceSymbols;
    @Value("${binance.ws.reconnect-delay-ms:3000}")
    private long reconnectDelayMs;

    private String buildCombinedStreamUrl(List<String> symbols) {
        StringJoiner joiner = new StringJoiner("/");
        for (String s : symbols) joiner.add(s.toLowerCase() + "@ticker");
        return binanceBaseUrl + joiner;
    }

    @PostConstruct
    public void start() {
        running = true;
        connect();
    }

    @PreDestroy
    public void stop() {
        running = false;
        try {
            WebSocket current = this.webSocket;
            if (current != null) {
                current.sendClose(WebSocket.NORMAL_CLOSURE, "application shutdown")
                        .exceptionally(ex -> null)
                        .join();
            }
        } catch (Exception e) {
            log.debug("Error while closing Binance WebSocket", e);
        }
        reconnectScheduler.shutdownNow();
    }

    private void connect() {
        if (!running) return;
        List<String> symbols = Arrays.asList(binanceSymbols.split(","));
        String url = buildCombinedStreamUrl(symbols);
        log.info("Connecting to Binance WS: {}", url);
        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(url), new BinanceListener())
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    reconnectScheduled.set(false);
                    log.info("Binance WS connected successfully");
                })
                .exceptionally(ex -> {
                    log.error("Binance WS connect failed", ex);
                    scheduleReconnect();
                    return null;
                });
    }

    private void scheduleReconnect() {
        if (!running || !reconnectScheduled.compareAndSet(false, true)) {
            return;
        }

        log.info("Scheduling Binance WS reconnect in {} ms", reconnectDelayMs);

        reconnectScheduler.schedule(() -> {
            reconnectScheduled.set(false);
            if (running) {
                connect();
            }
        }, reconnectDelayMs, TimeUnit.MILLISECONDS);
    }

    private class BinanceListener implements WebSocket.Listener {

        private final StringBuilder buffer = new StringBuilder();

        @Override
        public void onOpen(WebSocket webSocket) {
            log.info("Binance WS connection opened");
            webSocket.request(1);
        }

        @Override
        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            buffer.append(data);

            if (last) {
                String message = buffer.toString();
                buffer.setLength(0);
                handleMessage(message);
            }

            webSocket.request(1);
            return CompletableFuture.completedFuture(null);
        }

        private void handleMessage(String message) {
            try {
                BinanceCombinedStreamMessage parsed =
                        objectMapper.readValue(message, BinanceCombinedStreamMessage.class);

                BinanceTickerData data = parsed.data();
                if (data == null || data.symbol() == null || data.lastPrice() == null) {
                    return;
                }

                Instant eventTime = data.eventTime() > 0
                        ? Instant.ofEpochMilli(data.eventTime())
                        : Instant.now();

                // Set friendly name using the symbol map (normalize to uppercase and trim)
                String normalizedSymbol = data.symbol().toUpperCase().trim();
                String name = SymbolConstants.SYMBOL_NAMES.getOrDefault(normalizedSymbol, normalizedSymbol);

                MarketTick tick = MarketTick.builder()
                        .symbol(normalizedSymbol)
                        .name(name)
                        .price(data.lastPrice()) // keep original precision
                        .priceChange1m(BigDecimal.ZERO)  // placeholder for change 1m
                        .priceChange5m(BigDecimal.ZERO)  // placeholder for change 5m
                        .volume(data.volume() != null ? data.volume() : BigDecimal.ZERO)
                        .updatedAt(eventTime)
                        .build();

                marketStateService.updateTick(tick);

            } catch (Exception e) {
                log.warn("Failed to parse Binance WS message", e);
            }
        }

        @Override
        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            log.warn("Binance WS closed. statusCode={}, reason={}", statusCode, reason);
            BinanceWsClient.this.webSocket = null;
            scheduleReconnect();
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void onError(WebSocket webSocket, Throwable error) {
            log.error("Binance WS error", error);
            BinanceWsClient.this.webSocket = null;
            scheduleReconnect();
        }
    }
}