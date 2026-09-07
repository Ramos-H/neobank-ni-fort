package com.neobank.log_ai_service.summary;

import com.neobank.log_ai_service.config.SummarizerProperties;
import com.neobank.log_ai_service.gemini.GeminiClient;
import com.neobank.log_ai_service.loki.LokiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * The orchestrator: pull recent problem lines from Loki, hand them to Gemini
 * if there are any, store the result. Runs on a fixed schedule so the whole
 * pipeline works unattended during a demo — no manual step required.
 */
@Component
public class LogAnalysisScheduler {

    private static final Logger log = LoggerFactory.getLogger(LogAnalysisScheduler.class);

    // Caps how much raw log text we ever send to the model in one call —
    // keeps prompts small, cheap, and inside any provider's free-tier limits.
    private static final int MAX_LINES_TO_MODEL = 200;

    private final LokiClient lokiClient;
    private final GeminiClient geminiClient;
    private final LogSummaryStore store;
    private final SummarizerProperties properties;

    public LogAnalysisScheduler(LokiClient lokiClient, GeminiClient geminiClient,
                                 LogSummaryStore store, SummarizerProperties properties) {
        this.lokiClient = lokiClient;
        this.geminiClient = geminiClient;
        this.store = store;
        this.properties = properties;
    }

    @Scheduled(fixedRateString = "${ai.summarizer.fixed-rate-ms}")
    public void analyze() {
        runOnce();
    }

    /**
     * Split out from {@link #analyze()} so the controller can also trigger a
     * run on demand — handy for a live demo instead of waiting on the clock.
     */
    public LogSummary runOnce() {
        List<String> lines = lokiClient.queryRecentLogs(properties.logQuery(), properties.lookbackMinutes());

        if (lines.isEmpty()) {
            LogSummary noop = LogSummary.noAnomalies(Instant.now());
            store.save(noop);
            return noop;
        }

        List<String> sample = lines.size() > MAX_LINES_TO_MODEL
                ? lines.subList(0, MAX_LINES_TO_MODEL)
                : lines;

        String summaryText;
        try {
            summaryText = geminiClient.summarize(buildPrompt(sample));
        } catch (Exception e) {
            // Never let a flaky AI call take down the schedule — degrade to
            // "here's the raw count" instead of throwing.
            log.warn("Gemini summarization failed, storing raw counts only", e);
            summaryText = "AI summary unavailable this cycle (" + e.getMessage() + ") — "
                    + lines.size() + " matching log lines were found.";
        }

        LogSummary summary = new LogSummary(
                Instant.now(),
                lines.size(),
                summaryText,
                sample.subList(0, Math.min(5, sample.size())));
        store.save(summary);
        return summary;
    }

    private String buildPrompt(List<String> lines) {
        return """
                You are monitoring logs for a small banking microservices demo (Singko de Bangko).
                Below are recent ERROR/WARN log lines pulled from all 5 services. In under 150 words:
                1) Group them into distinct issues (don't just repeat every line).
                2) Call out anything that looks like a real bug vs. expected/noisy warnings.
                3) Suggest the single most useful next debugging step.

                LOG LINES:
                %s
                """.formatted(String.join("\n", lines));
    }
}
