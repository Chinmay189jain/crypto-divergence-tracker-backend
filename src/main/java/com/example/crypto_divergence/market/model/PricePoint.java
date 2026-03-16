package com.example.crypto_divergence.market.model;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Represents a price point at a specific timestamp for historical tracking.
 */
public record PricePoint(Instant ts, BigDecimal price) {}

