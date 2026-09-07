package com.neobank.log_ai_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Binds the `loki.*` keys from application.yaml.
@ConfigurationProperties(prefix = "loki")
public record LokiProperties(String baseUrl) {
}
