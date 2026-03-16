package com.example.crypto_divergence.market.constants;

import java.util.Map;

/**
 * Contains symbol to friendly name mapping for API responses.
 */
public class SymbolConstants {
    public static final Map<String, String> SYMBOL_NAMES = Map.of(
        "BTCUSDT", "Bitcoin",
        "ETHUSDT", "Ethereum",
        "SOLUSDT", "Solana",
        "BNBUSDT", "BNB",
        "XRPUSDT", "XRP",
        "ADAUSDT", "Cardano",
        "DOGEUSDT", "Dogecoin",
        "MATICUSDT", "Polygon",
        "DOTUSDT", "Polkadot",
        "LINKUSDT", "Chainlink"
    );
}

