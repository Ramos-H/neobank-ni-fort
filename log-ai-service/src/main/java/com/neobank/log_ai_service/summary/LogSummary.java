package com.neobank.log_ai_service.summary;

import java.time.Instant;
import java.util.List;

/**
 * One run's result: when it ran, how many matching log lines were found, the
 * AI's summary text, and a small sample of the raw lines for context.
 */
public record LogSummary(
        Instant generatedAt,
        int rawLogCount,
        String summaryText,
        List<String> sampleLines
) {
    // Used when a run finds zero ERROR/WARN lines — skips calling Gemini
    // entirely (saves free-tier quota) and just reports a clean bill of health.
    public static LogSummary noAnomalies(Instant now) {
        return new LogSummary(now, 0, "No ERROR/WARN log lines in this window — stack looks healthy.", List.of());
    }
}
