package com.neobank.log_ai_service.web;

import com.neobank.log_ai_service.summary.LogAnalysisScheduler;
import com.neobank.log_ai_service.summary.LogSummary;
import com.neobank.log_ai_service.summary.LogSummaryStore;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Public surface of this service. Not routed through the api-gateway and not
 * JWT-protected — same trust tier as /actuator/prometheus: reachable only
 * inside the Docker network (or via localhost:8085 for local debugging), and
 * carries no customer data, so it doesn't need the gateway's auth.
 */
@RestController
@RequestMapping("/api/ai-logs")
public class LogAiController {

    private final LogSummaryStore store;
    private final LogAnalysisScheduler scheduler;

    public LogAiController(LogSummaryStore store, LogAnalysisScheduler scheduler) {
        this.store = store;
        this.scheduler = scheduler;
    }

    @GetMapping("/summary")
    public LogSummary latestSummary() {
        return store.latest();
    }

    @GetMapping("/history")
    public List<LogSummary> history() {
        return store.history();
    }

    // Manual trigger — useful in a sprint demo so you're not waiting on the
    // schedule to show the feature working live.
    @PostMapping("/analyze-now")
    public LogSummary analyzeNow() {
        return scheduler.runOnce();
    }
}
