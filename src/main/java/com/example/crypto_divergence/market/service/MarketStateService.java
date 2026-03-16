package com.example.crypto_divergence.market.service;

import com.example.crypto_divergence.market.model.MarketTick;
import com.example.crypto_divergence.market.model.PricePoint;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import com.example.crypto_divergence.market.model.TrendDirection;

/**
 * Thread-safe in-memory cache for the latest market tick per symbol.
 * Also maintains a small price history for each symbol to compute 1m and 5m percent changes.
 */
@Service
public class MarketStateService {

    // Latest tick per symbol
    private final Map<String, MarketTick> latestTicks = new ConcurrentHashMap<>();

    // Price history per symbol (for 1m/5m change calculation)
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<PricePoint>> history = new ConcurrentHashMap<>();

    /**
     * Updates the latest tick for a symbol, stores price history, and computes 1m/5m percent changes.
     * @param tick The new market tick
     */
    public void updateTick(MarketTick tick) {
        Objects.requireNonNull(tick, "tick must not be null");
        String normalizedSymbol = normalizeSymbol(tick.getSymbol());
        Instant now = tick.getUpdatedAt();
        BigDecimal priceNow = tick.getPrice();

        // Update price history for this symbol
        ConcurrentLinkedDeque<PricePoint> deque = history.computeIfAbsent(normalizedSymbol, k -> new ConcurrentLinkedDeque<>());
        deque.addLast(new PricePoint(now, priceNow));

        // Remove entries older than 6 minutes (buffer for 5m calculation)
        Instant cutoff = now.minus(Duration.ofMinutes(6));
        while (!deque.isEmpty() && deque.peekFirst().ts().isBefore(cutoff)) {
            deque.pollFirst();
        }

        // Compute 1m and 5m percent changes
        BigDecimal change1m = computePercentChange(deque, now.minus(Duration.ofMinutes(1)), priceNow);
        BigDecimal change5m = computePercentChange(deque, now.minus(Duration.ofMinutes(5)), priceNow);

        // Compute trend direction based on 1m% change
        TrendDirection trend;
        BigDecimal threshold = new BigDecimal("0.10"); // 0.10% beginner-friendly threshold
        if (change1m.compareTo(threshold) >= 0) {
            trend = TrendDirection.UP;
        } else if (change1m.compareTo(threshold.negate()) <= 0) {
            trend = TrendDirection.DOWN;
        } else {
            trend = TrendDirection.FLAT;
        }

        // Create a new MarketTick with computed changes
        MarketTick updatedTick = MarketTick.builder()
                .symbol(tick.getSymbol())
                .name(tick.getName()) // Preserve friendly name
                .price(tick.getPrice())
                .priceChange1m(change1m)
                .priceChange5m(change5m)
                .volume(tick.getVolume())
                .updatedAt(tick.getUpdatedAt())
                .trend(trend)
                .build();
        latestTicks.put(normalizedSymbol, updatedTick);
    }

    /**
     * Gets the latest tick for a symbol.
     */
    public Optional<MarketTick> getLatest(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(latestTicks.get(normalizeSymbol(symbol)));
    }

    /**
     * Gets a stable snapshot of all latest ticks.
     */
    public List<MarketTick> getAllLatest() {
        return latestTicks.values()
                .stream()
                .sorted(Comparator.comparing(MarketTick::getSymbol))
                .toList();
    }

    /**
     * Finds the closest price at or before the target time and computes percent change.
     * @param deque The price history deque
     * @param targetTime The target time (1m or 5m ago)
     * @param priceNow The current price
     * @return Percent change, or 0.00 if not enough history
     */
    private BigDecimal computePercentChange(ConcurrentLinkedDeque<PricePoint> deque, Instant targetTime, BigDecimal priceNow) {
        PricePoint closest = null;
        for (PricePoint pp : deque) {
            if (!pp.ts().isAfter(targetTime)) {
                closest = pp;
            } else {
                break;
            }
        }
        if (closest == null || closest.price().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        // ((priceNow - priceThen) / priceThen) * 100
        return priceNow.subtract(closest.price())
                .divide(closest.price(), 6, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private String normalizeSymbol(String symbol) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        return symbol.trim().toUpperCase();
    }
}