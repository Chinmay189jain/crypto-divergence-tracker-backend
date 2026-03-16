package com.example.crypto_divergence.market.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a market tick (latest price and stats for a symbol).
 * Uses Lombok for boilerplate code reduction and builder pattern.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MarketTick {
    private String symbol;
    private String name; // Friendly name for the symbol
    private BigDecimal price;
    private BigDecimal priceChange1m; // 1-minute percent change
    private BigDecimal priceChange5m; // 5-minute percent change
    private BigDecimal volume;
    private Instant updatedAt;
    private TrendDirection trend;
}
