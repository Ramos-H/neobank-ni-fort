package com.neobank.log_ai_service.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// Binds the `ai.gemini.*` keys from application.yaml — swap models or rotate
// the key by changing an env var, no code changes needed.
@ConfigurationProperties(prefix = "ai.gemini")
public record GeminiProperties(String apiKey, String model) {
}
