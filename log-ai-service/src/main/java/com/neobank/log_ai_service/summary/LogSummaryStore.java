package com.neobank.log_ai_service.summary;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * In-memory holder for the latest + recent summaries. Deliberately no
 * database: this is a transient dashboard signal, not a system of record, so
 * it resets on restart on purpose. If you later need history to survive a
 * restart, that's a one-line JPA entity away — this class is the only place
 * you'd touch.
 */
@Component
public class LogSummaryStore {

    private static final int MAX_HISTORY = 20;

    private final AtomicReference<LogSummary> latest =
            new AtomicReference<>(LogSummary.noAnomalies(Instant.now()));
    private final List<LogSummary> history = Collections.synchronizedList(new ArrayList<>());

    public void save(LogSummary summary) {
        latest.set(summary);
        synchronized (history) {
            history.add(summary);
            while (history.size() > MAX_HISTORY) {
                history.remove(0);
            }
        }
    }

    public LogSummary latest() {
        return latest.get();
    }

    public List<LogSummary> history() {
        synchronized (history) {
            return List.copyOf(history);
        }
    }
}
