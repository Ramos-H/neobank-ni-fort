package com.neobank.log_ai_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Binds the `ai.summarizer.*` keys from application.yaml.
@ConfigurationProperties(prefix = "ai.summarizer")
public record SummarizerProperties(long fixedRateMs, int lookbackMinutes, String logQuery) {
}
