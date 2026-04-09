package com.example.crypto_divergence.divergence.controller;

import com.example.crypto_divergence.divergence.service.DivergenceDetector;
import com.example.crypto_divergence.divergence.service.DivergenceDetector.Alert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/divergence")
public class DivergenceController {
    @Autowired
    private DivergenceDetector divergenceDetector;

    @GetMapping("/alerts/active")
    public List<Alert> getActiveAlerts() {
        return divergenceDetector.getActiveAlerts();
    }

    @GetMapping("/alerts/history")
    public List<Alert> getAlertHistory() {
        return divergenceDetector.getAlertHistory();
    }
}

