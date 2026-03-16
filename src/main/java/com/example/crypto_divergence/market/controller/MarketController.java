package com.example.crypto_divergence.market.controller;

import com.example.crypto_divergence.market.model.MarketTick;
import com.example.crypto_divergence.market.service.MarketStateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for accessing the latest market data.
 */
@RestController
@RequestMapping("/api/market")
@RequiredArgsConstructor
public class MarketController {

    private final MarketStateService marketStateService;

    /**
     * Get the latest ticks for all tracked symbols.
     */
    @GetMapping("/latest")
    public ResponseEntity<List<MarketTick>> getAllLatest() {
        return ResponseEntity.ok(marketStateService.getAllLatest());
    }

    /**
     * Get the latest tick for a specific symbol.
     */
    @GetMapping("/latest/{symbol}")
    public ResponseEntity<MarketTick> getLatestBySymbol(@PathVariable String symbol) {
        return marketStateService.getLatest(symbol)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}