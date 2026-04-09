package com.example.crypto_divergence.sentiment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SentimentDTO {
    private String symbol;
    private String name;
    private double sentimentScore;
    private String status;
    private String timestamp;
}
