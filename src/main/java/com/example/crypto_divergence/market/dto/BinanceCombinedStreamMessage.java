package com.example.crypto_divergence.market.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record BinanceCombinedStreamMessage(
        @NotNull @JsonProperty("stream") String stream,
        @NotNull @Valid @JsonProperty("data") BinanceTickerData data
) {}
