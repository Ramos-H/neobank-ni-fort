# Story 4 — Observability Stack Decision

**Status:** Decided, not yet implemented (pending explicit go-ahead)
**Scope:** Metrics + dashboards only. AI log analysis (Loki + Promtail + log-ai-service) is already done in a separate slice — see `README-ai-log-analysis.md`.

## Context

The team explored 4 options for observability via another AI session (see the
attached "Singko de Bangko Observability Guide" transcript):

1. Spring Boot 4 native OpenTelemetry starter + Grafana LGTM stack (Loki, Grafana, Tempo, Mimir)
2. DIY classic stack: Prometheus + Grafana (metrics), Zipkin (traces), Loki+Promtail (logs)
3. OpenTelemetry Collector + all-in-one APM (SigNoz / Jaeger)
4. Managed SaaS (Grafana Cloud free tier, Datadog, New Relic, Honeycomb)

That exploration happened before `log-ai-service` + Loki + Promtail were actually
built and merged into this repo. Re-evaluating now that they exist:

- The repo already runs 7 containers for Stories 1–3 (5 services + 2 Postgres),
  plus `loki` + `promtail` + `log-ai-service` = **10 containers before adding
  anything for metrics.**
- Option 1's LGTM bundle would add a **second, redundant Loki** alongside the
  one already collecting logs for `log-ai-service`, plus Tempo and Mimir for
  a tracing signal the DoD doesn't actually require.
- The Story 4 Definition of Done (`Architecture_and_Techstack.md`, section 5)
  asks for: `/actuator/prometheus` on all 5 services, Prometheus scraping them,
  and a Grafana dashboard showing service health / request rates / JVM metrics.
  **No distributed tracing requirement.**

## Decision

Use a **trimmed version of Option 2**: Prometheus + Grafana for metrics,
reusing the Loki that already exists for logs. No tracing backend for now.

Concretely, this means:

- Add `spring-boot-starter-actuator` + `micrometer-registry-prometheus` to
  all 5 core services (`discovery-server`, `api-gateway`, `auth-service`,
  `account-service`, `transaction-service`). One dependency, no code changes.
- Add a `prometheus` container that scrapes `/actuator/prometheus` on all 5.
- Add a `grafana` container, provisioned with **two datasources**:
  Prometheus (metrics) and the existing Loki (logs) — one UI for both
  signals without standing up a second log pipeline.
- Ship this as its own `docker-compose.observability-metrics.yml` (or fold
  into the existing `docker-compose.observability.yml` — TBD at implementation
  time) so it stays a `-f` overlay, same pattern as the log-analysis slice,
  and doesn't create merge conflicts with Stories 1–3.

## Explicitly deferred (not this pass)

- **Distributed tracing** (OpenTelemetry starter + Zipkin or Tempo). Not in
  the DoD. Cheapest path if picked up later: `spring-boot-starter-opentelemetry`
  on the 5 services + a single Tempo (or Zipkin) container — no Mimir, no
  second Loki.
- Options 3 (SigNoz/Collector) and 4 (managed SaaS) — rejected: SigNoz's
  ClickHouse backend is the heaviest of the four options on a constrained
  laptop; managed SaaS needs a live internet connection and an account
  during the sprint demo, which is a single point of failure.

## Rejected alternatives — why

| Option | Why not (given current repo state) |
|---|---|
| 1 — OTel + full LGTM | Duplicates the existing Loki; Tempo+Mimir add tracing the DoD doesn't ask for; +4 containers |
| 3 — Collector + SigNoz | Heaviest option (ClickHouse); smaller community; +2 containers incl. a new moving part |
| 4 — Managed SaaS | Zero containers, but demo now depends on internet + a live account; thematically odd for an offline bank demo |

## Next step

Waiting on explicit go-ahead to implement: `pom.xml` additions, `prometheus.yml`
scrape config, the new compose overlay, and Grafana datasource provisioning.
