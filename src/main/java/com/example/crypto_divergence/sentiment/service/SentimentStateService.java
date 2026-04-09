package com.example.crypto_divergence.sentiment.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.Random;

@Service
public class SentimentStateService {
    private final Map<String, Double> sentimentScores = new ConcurrentHashMap<>();
    private final Map<String, Long> sentimentTimestamps = new ConcurrentHashMap<>();
    private final Random random = new Random();

    // Mock generator: update scores every 15 seconds for all tracked symbols
    @Scheduled(fixedDelay = 15000)
    public void generateMockSentiment() {
        // Use all symbols from market state
        for (String symbol : com.example.crypto_divergence.market.constants.SymbolConstants.SYMBOL_NAMES.keySet()) {
            double score = -1 + 2 * random.nextDouble(); // [-1, +1]
            sentimentScores.put(symbol.trim().toUpperCase(), score);
            sentimentTimestamps.put(symbol.trim().toUpperCase(), System.currentTimeMillis());
        }
    }

    public Double getSentimentScore(String symbol) {
        return sentimentScores.get(symbol.trim().toUpperCase());
    }

    public Long getSentimentTimestamp(String symbol) {
        return sentimentTimestamps.get(symbol.trim().toUpperCase());
    }

    public Map<String, Double> getAllSentimentScores() {
        return sentimentScores;
    }

    public Map<String, Long> getAllSentimentTimestamps() {
        return sentimentTimestamps;
    }
}
