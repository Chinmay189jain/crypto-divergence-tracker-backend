package com.example.crypto_divergence.market.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BinanceTickerData(
        @NotNull @JsonProperty("s") String symbol,
        @NotNull @DecimalMin("0.0") @JsonProperty("c") BigDecimal lastPrice,
        @NotNull @JsonProperty("P") BigDecimal priceChangePercent,
        @NotNull @DecimalMin("0.0") @JsonProperty("v") BigDecimal volume,
        @NotNull @JsonProperty("E") long eventTime
) {}
