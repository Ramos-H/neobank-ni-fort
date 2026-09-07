package com.neobank.log_ai_service.gemini;

import com.neobank.log_ai_service.config.GeminiProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Thin wrapper around Gemini's `generateContent` REST endpoint
 * (docs: https://ai.google.dev/api/generate-content). Only knows how to turn
 * a prompt string into a response string — no Loki/log knowledge lives here,
 * so swapping in Groq/OpenRouter later means changing only this one class.
 */
@Component
public class GeminiClient {

    private static final String ENDPOINT_TEMPLATE =
            "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent";

    private final RestClient restClient = RestClient.create();
    private final GeminiProperties properties;

    public GeminiClient(GeminiProperties properties) {
        this.properties = properties;
    }

    @SuppressWarnings("unchecked")
    public String summarize(String prompt) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not set — get a free key at https://aistudio.google.com/apikey "
                            + "and put it in your .env file (see .env.example).");
        }

        String url = ENDPOINT_TEMPLATE.formatted(properties.model());

        // Gemini's request shape: { "contents": [ { "parts": [ {"text": "..."} ] } ] }
        Map<String, Object> requestBody = Map.of(
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", prompt)))
                )
        );

        Map<String, Object> response = restClient.post()
                .uri(url)
                .header("x-goog-api-key", properties.apiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response == null) {
            throw new IllegalStateException("Gemini returned an empty response");
        }

        // Response shape: { "candidates": [ { "content": { "parts": [ {"text": "..."} ] } } ] }
        List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalStateException("Gemini returned no candidates: " + response);
        }

        Map<String, Object> content = (Map<String, Object>) candidates.get(0).get("content");
        List<Map<String, Object>> parts = (List<Map<String, Object>>) content.get("parts");

        StringBuilder text = new StringBuilder();
        if (parts != null) {
            for (Map<String, Object> part : parts) {
                Object t = part.get("text");
                if (t != null) {
                    text.append(t);
                }
            }
        }
        return text.toString();
    }
}
