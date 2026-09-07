package com.neobank.log_ai_service.loki;

import com.neobank.log_ai_service.config.LokiProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Talks to Loki's HTTP query API (docs: https://grafana.com/docs/loki/latest/reference/loki-http-api/).
 * This is the ONLY class in the service that knows Loki's JSON shape —
 * everyone downstream just gets a plain List<String> of log lines.
 */
@Component
public class LokiClient {

    private final RestClient restClient;

    public LokiClient(LokiProperties lokiProperties) {
        this.restClient = RestClient.create(lokiProperties.baseUrl());
    }

    /**
     * Runs a LogQL range query and flattens every matching stream's log lines
     * into one list (newest-within-stream order; streams themselves are not
     * globally re-sorted — fine for "recent problems", not for exact ordering).
     *
     * @param logQlQuery      e.g. {service=~".+"} |~ `(?i)error|exception`
     * @param sinceMinutesAgo how far back from "now" to search
     */
    @SuppressWarnings("unchecked")
    public List<String> queryRecentLogs(String logQlQuery, int sinceMinutesAgo) {
        Instant end = Instant.now();
        Instant start = end.minusSeconds(sinceMinutesAgo * 60L);

        // Loki's query_range wants unix-nanosecond timestamps for start/end.
        Map<String, Object> response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/loki/api/v1/query_range")
                        .queryParam("query", logQlQuery)
                        .queryParam("start", start.getEpochSecond() + "000000000")
                        .queryParam("end", end.getEpochSecond() + "000000000")
                        .queryParam("limit", 500)
                        .build())
                .retrieve()
                .body(Map.class);

        List<String> lines = new ArrayList<>();
        if (response == null) {
            return lines;
        }

        Map<String, Object> data = (Map<String, Object>) response.get("data");
        if (data == null) {
            return lines;
        }

        List<Map<String, Object>> streams = (List<Map<String, Object>>) data.get("result");
        if (streams == null) {
            return lines;
        }

        // Each "stream" is one label-set (roughly: one container); "values"
        // is a list of [timestampNanosAsString, logLine] pairs.
        for (Map<String, Object> stream : streams) {
            List<List<String>> values = (List<List<String>>) stream.get("values");
            if (values == null) {
                continue;
            }
            for (List<String> pair : values) {
                if (pair.size() == 2) {
                    lines.add(pair.get(1));
                }
            }
        }
        return lines;
    }
}
