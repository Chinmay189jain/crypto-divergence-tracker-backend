package com.example.crypto_divergence.sentiment.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import com.example.crypto_divergence.sentiment.service.SentimentStateService;
import com.example.crypto_divergence.sentiment.dto.SentimentDTO;
import com.example.crypto_divergence.market.constants.SymbolConstants;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/api/sentiment")
public class SentimentController {
    @Autowired
    private SentimentStateService sentimentStateService;

    @GetMapping("/latest")
    public List<SentimentDTO> getAllSentimentScores() {
        Map<String, Double> scores = sentimentStateService.getAllSentimentScores();
        Map<String, Long> timestamps = sentimentStateService.getAllSentimentTimestamps();
        List<SentimentDTO> result = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
        for (String symbol : scores.keySet()) {
            double score = scores.get(symbol);
            String status = score > 0 ? "Positive" : (score < 0 ? "Negative" : "Neutral");
            String friendlyName = SymbolConstants.SYMBOL_NAMES.getOrDefault(symbol, symbol);
            Long ts = timestamps.get(symbol);
            String timestamp = ts != null ? formatter.format(Instant.ofEpochMilli(ts)) : null;
            result.add(new SentimentDTO(symbol, friendlyName, score, status, timestamp));
        }
        return result;
    }

    @GetMapping("/latest/{symbol}")
    public Double getSentimentScore(@PathVariable String symbol) {
        return sentimentStateService.getSentimentScore(symbol);
    }
}
