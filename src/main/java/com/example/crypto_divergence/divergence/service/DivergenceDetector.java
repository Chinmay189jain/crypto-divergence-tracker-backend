package com.example.crypto_divergence.divergence.service;

import com.example.crypto_divergence.market.service.MarketStateService;
import com.example.crypto_divergence.sentiment.service.SentimentStateService;
import com.example.crypto_divergence.market.model.MarketTick;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.math.BigDecimal;
import java.util.Locale;

@Service
public class DivergenceDetector {
            private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(DivergenceDetector.class);
    @Autowired
    private MarketStateService marketStateService;
    @Autowired
    private SentimentStateService sentimentStateService;

    // Alert state
    private final Map<String, Alert> activeAlerts = new ConcurrentHashMap<>();
    private final Deque<Alert> alertHistory = new ArrayDeque<>();
    private static final int HISTORY_LIMIT = 500;
    private static final long COOLDOWN_MS = 60000;

    // Thresholds
    // Lowered thresholds for easier testing
    private static final double PRICE_THRESHOLD = 0.01;
    private static final double SENTIMENT_THRESHOLD = 0.01;

    public void detectDivergence() {
        Map<String, MarketTick> marketTicks = getAllLatestTicks();
        Map<String, Double> sentimentScores = sentimentStateService.getAllSentimentScores();
        long now = System.currentTimeMillis();
        for (String symbol : marketTicks.keySet()) {
            MarketTick tick = marketTicks.get(symbol);
            Double sentiment = sentimentScores.get(symbol);
            if (tick == null || sentiment == null) continue;
            BigDecimal change5mBD = tick.getPriceChange5m();
            double change5m = change5mBD != null ? change5mBD.doubleValue() : 0.0;
            Alert existing = activeAlerts.get(symbol);
            boolean divergence = false;
            boolean bullish = change5m <= -PRICE_THRESHOLD && sentiment >= SENTIMENT_THRESHOLD;
            boolean bearish = change5m >= PRICE_THRESHOLD && sentiment <= -SENTIMENT_THRESHOLD;
            if (bullish || bearish) divergence = true;
            if (divergence) {
                if (existing == null || existing.status == AlertStatus.RESOLVED && now - existing.resolvedAt >= COOLDOWN_MS) {
                    Alert alert = new Alert(symbol, change5m, sentiment, now, getSeverity(change5m, sentiment));
                    activeAlerts.put(symbol, alert);
                    alertHistory.addLast(alert);
                    if (alertHistory.size() > HISTORY_LIMIT) alertHistory.removeFirst();
                    log.info("Divergence alert generated: {} priceChange5m={} sentimentScore={} severity={}", symbol, change5m, sentiment, alert.severity);
                }
            } else {
                if (existing != null && existing.status == AlertStatus.ACTIVE) {
                    existing.status = AlertStatus.RESOLVED;
                    existing.resolvedAt = now;
                    log.info("Divergence alert resolved: {}", symbol);
                }
            }
        }
    }

    private Map<String, MarketTick> getAllLatestTicks() {
        // TODO: Replace with actual getter from MarketStateService
        // If not present, add a public getter in MarketStateService
        try {
            java.lang.reflect.Field field = MarketStateService.class.getDeclaredField("latestTicks");
            field.setAccessible(true);
            return (Map<String, MarketTick>) field.get(marketStateService);
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    public List<Alert> getActiveAlerts() {
        List<Alert> result = new ArrayList<>();
        for (Alert alert : activeAlerts.values()) {
            if (alert.status == AlertStatus.ACTIVE) result.add(alert);
        }
        return result;
    }

    public List<Alert> getAlertHistory() {
        return new ArrayList<>(alertHistory);
    }

    private Severity getSeverity(double priceChange, double sentiment) {
        double absPrice = Math.abs(priceChange);
        double absSentiment = Math.abs(sentiment);
        if (absPrice >= 2.0 || absSentiment >= 0.6) return Severity.HIGH;
        if (absPrice >= 1.0 || absSentiment >= 0.45) return Severity.MEDIUM;
        return Severity.LOW;
    }

    // Run divergence detection every second
    @Scheduled(fixedDelay = 1000)
    public void scheduledDetectDivergence() {
        detectDivergence();
    }

    public static class Alert {
        public final String symbol;
        public final double priceChange;
        public final double sentimentScore;
        public final long lastTriggeredAt;
        public final AlertType type;
        public final String message;
        public final String timeframe;
        public Severity severity;
        public AlertStatus status;
        public long resolvedAt;
        public Alert(String symbol, double priceChange, double sentimentScore, long lastTriggeredAt, Severity severity) {
            this.symbol = symbol;
            this.priceChange = priceChange;
            this.sentimentScore = sentimentScore;
            this.lastTriggeredAt = lastTriggeredAt;
            this.type = deriveType(priceChange, sentimentScore);
            this.timeframe = "5m";
            this.message = buildMessage(priceChange, sentimentScore, this.timeframe);
            this.severity = severity;
            this.status = AlertStatus.ACTIVE;
            this.resolvedAt = 0;
        }
    }

    private static AlertType deriveType(double priceChange, double sentimentScore) {
        if (priceChange > 0 && sentimentScore < 0) {
            return AlertType.BEARISH;
        }
        return AlertType.BULLISH;
    }

    private static String buildMessage(double priceChange, double sentimentScore, String timeframe) {
        String priceArrow = priceChange >= 0 ? "↑" : "↓";
        String sentimentArrow = sentimentScore >= 0 ? "↑" : "↓";
        return String.format(
                Locale.US,
                "Price %s %.2f%% (%s) but Sentiment %s %.2f",
                priceArrow,
                Math.abs(priceChange),
                timeframe,
                sentimentArrow,
                Math.abs(sentimentScore)
        );
    }

    public enum AlertStatus { ACTIVE, RESOLVED }
    public enum Severity { LOW, MEDIUM, HIGH }
    public enum AlertType { BULLISH, BEARISH }
}
