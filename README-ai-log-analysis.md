# AI Log Analysis — Story 4 added scope

## What this adds

Three new pieces, none of which touch `auth-service`, `account-service`,
`transaction-service`, `api-gateway`, or `discovery-server`:

| Piece | What it does |
|---|---|
| `loki/` (container, from `docker-compose.observability.yml`) | Stores logs, queryable via LogQL |
| `promtail/` (container, same file) | Auto-discovers every running container and ships its stdout logs into Loki — no code changes needed in any of the 5 existing services |
| `log-ai-service/` (new Spring Boot service) | Every 2 minutes (configurable), pulls recent ERROR/WARN lines from Loki, sends them to Gemini for a plain-English summary, and exposes the result over a small REST API |

This is the **log** leg of the Option 1 stack you already committed to
(native OpenTelemetry + Grafana LGTM). It only stands up Loki, not the full
LGTM bundle — Prometheus/Grafana/Tempo/Mimir are metrics+traces territory,
presumably someone else's slice of Story 4. If a teammate later adds Grafana,
point a new Grafana datasource at `http://loki:3100` and their dashboards can
show these same logs — nothing here needs to change for that to work.

## How the pieces fit together

```
account-service, auth-service, ... (stdout logs, unmodified)
        │
        ▼
   promtail  ──ships──▶  loki  ◀──queries (LogQL)──  log-ai-service
                                                            │
                                                    calls Gemini API
                                                            │
                                                     GET /api/ai-logs/summary
```

## Where everything goes

Drop these into your existing repo root, next to `docker-compose.yml`:

```
neobank-ni-fort/
├── docker-compose.yml              (existing — untouched)
├── docker-compose.observability.yml  ← new
├── .env.example                      ← new
├── observability/
│   ├── loki-config.yml               ← new
│   └── promtail-config.yml           ← new
├── log-ai-service/                   ← new (whole Spring Boot service)
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/...
├── discovery-server/                (existing)
├── api-gateway/                     (existing)
├── auth-service/                    (existing)
├── account-service/                 (existing)
└── transaction-service/             (existing)
```

## Running it

1. Get a free Gemini API key: https://aistudio.google.com/apikey
2. `cp .env.example .env` and paste the key in.
3. `docker compose -f docker-compose.yml -f docker-compose.observability.yml up --build`
4. Generate some logs — log in, check a balance, try a transfer with insufficient
   funds (that one's a guaranteed WARN/ERROR line from `account-service`).
5. Either wait ~2 minutes for the scheduled run, or trigger it immediately for
   the demo:
   ```
   curl -X POST http://localhost:8085/api/ai-logs/analyze-now
   ```
6. Check the result:
   ```
   curl http://localhost:8085/api/ai-logs/summary
   curl http://localhost:8085/api/ai-logs/history
   ```

## What to double-check before the sprint review

I don't have network access in this sandbox, so none of this has actually been
compiled or run — please `mvn clean package` / `docker compose up --build` it
yourself before you rely on it. Two spots are the most likely to need a small
fix on a real machine, in order of likelihood:

1. **`observability/promtail-config.yml` mounts** — the
   `/var/lib/docker/containers` bind mount is the standard recipe and should
   work on Docker Desktop, but it's the one piece most sensitive to host OS
   quirks. If `docker compose logs promtail` shows no scrape targets, that's
   the first thing to check.
2. **`observability/loki-config.yml` `schema_config`** — this is the
   long-standing "getting started" shape; if the pulled Loki image complains
   on startup about the schema version, its own error message tells you
   exactly which field to bump.

The Gemini request/response shape in `GeminiClient.java` and the Loki
query API shape in `LokiClient.java` were both checked against current docs,
but a live end-to-end run is still worth doing before demo day rather than
trusting this note.

## Extension ideas (not built, in case you have time left)

- Persist `LogSummary` history to Postgres instead of in-memory (survives restarts)
- Push the summary to a Slack webhook instead of/alongside the REST endpoint
- Add a `severity` field the AI assigns per summary, so you can alert only on high-severity runs
