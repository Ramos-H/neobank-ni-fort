# Metrics & Dashboards — Story 4 added scope

See `observability-decision.md` for *why* this is Prometheus + Grafana and
not the fuller OpenTelemetry+Tempo+Mimir stack that came up earlier. Short
version: it reuses the Loki that already exists for `log-ai-service` instead
of duplicating it, and skips tracing because it isn't part of Story 4's
Definition of Done.

## What this adds

Two new pieces, plus a small addition to each of the 5 existing services:

| Piece | What it does |
|---|---|
| `spring-boot-starter-actuator` + `micrometer-registry-prometheus` (added to all 5 services' `pom.xml` + `application.yaml`) | Exposes `/actuator/prometheus` on each service — the metrics themselves, in the text format Prometheus expects |
| `prometheus` (container, from `docker-compose.observability.yml`) | Scrapes those 5 endpoints every 15s and stores the time series |
| `grafana` (container, same file) | Renders dashboards from Prometheus (metrics) **and** the existing Loki (logs) — one UI, two datasources, both auto-provisioned on startup |

None of the 5 services needed new Java code — Actuator + Micrometer's
Prometheus registry generate the `/actuator/prometheus` endpoint from a
dependency + a config block alone.

## How the pieces fit together

```
discovery-server, api-gateway, auth-service,          (existing — only their
account-service, transaction-service                   pom.xml/application.yaml
        │  /actuator/prometheus                         gained a few lines)
        ▼
   prometheus  ──scraped by──▶  grafana  ◀──also queries──  loki (existing,
                                    │                         from the AI-log
                                    ▼                         slice — unchanged)
                          http://localhost:3000
```

## Where everything goes

Drop these into your existing repo root, next to `docker-compose.yml`:

```
neobank-ni-fort/
├── docker-compose.yml                        (existing — untouched)
├── docker-compose.observability.yml          (existing — extended: +prometheus, +grafana)
├── .env.example                              (existing — extended: optional Grafana creds)
├── discovery-server/
│   ├── pom.xml                               (existing — extended: +actuator, +micrometer-prometheus)
│   └── src/main/resources/application.yaml   (existing — extended: +management block)
├── api-gateway/                              (same two files, same additions)
├── auth-service/                             (same two files, same additions)
├── account-service/                          (same two files, same additions)
├── transaction-service/                      (same two files, same additions)
└── observability/
    ├── loki-config.yml                       (existing — untouched)
    ├── promtail-config.yml                   (existing — untouched)
    ├── prometheus.yml                        (new — scrape targets for all 5 services)
    └── grafana/
        ├── provisioning/
        │   ├── datasources/datasources.yml   (new — wires up Prometheus + Loki automatically)
        │   └── dashboards/dashboards.yml     (new — tells Grafana to auto-load the dashboard below)
        └── dashboards/
            └── story4-service-overview.json  (new — starter dashboard, loads on first boot)
```

If a teammate's Stories 1-3 branch already changed a service's `pom.xml` or
`application.yaml` by the time you merge, treat these as an **addition**, not
a replacement — copy just the new `<dependency>` blocks / `management:` block
in, don't overwrite their file wholesale. Every patch above was written as an
append/insert for exactly this reason (see the comments inline in each file).

## Running it

Same command as the AI-log slice — this is one overlay file, so both slices
come up together:

```bash
docker compose -f docker-compose.yml -f docker-compose.observability.yml up --build
```

Then:

1. **Prometheus targets** — `http://localhost:9090/targets`. All 5 services
   should show `UP` once they've registered with Eureka and started serving
   traffic. A service showing `DOWN` almost always means it hasn't finished
   starting yet, not that something is misconfigured — give it a few seconds
   and refresh.
2. **Grafana dashboard** — `http://localhost:3000` (login `admin` / `admin`
   unless you set `GRAFANA_ADMIN_USER` / `GRAFANA_ADMIN_PASSWORD` in `.env`).
   Open **Dashboards → Singko de Bangko → Singko de Bangko - Service
   Overview** — it's already there, no manual import needed. Panels:
   - Service Health (up/down per service)
   - HTTP Request Rate
   - HTTP Request Latency (avg)
   - JVM Heap Used
   - Live Logs (same Loki data `log-ai-service` reads — confirms both
     datasources are wired correctly)
3. Generate traffic through Postman (login → balance → transfer, per the
   implementation guide) and watch the request-rate/latency panels move.

## Troubleshooting

- **A panel says "No data"** — check the Prometheus target for that service
  isn't `DOWN` first (step 1 above); a dashboard can't show metrics from a
  service Prometheus can't reach.
- **Grafana shows no datasources / no dashboard** — provisioning only runs
  on container *start*. If you edited a file under `observability/grafana/`
  after Grafana was already running, restart just that container:
  `docker compose restart grafana`.
- **Port already in use (3000 or 9090)** — something else on your machine is
  already bound to that port; stop it, or remap the left-hand side of the
  port mapping in `docker-compose.observability.yml` (e.g. `"3001:3000"`).
