# ADR 0005 — Observability correlated by traceId

## Status
Accepted

## Context
A failed Pix iniciação can fail at many places: cache (stale entry), gateway (mTLS handshake, HTTP error, deserialization), DICT itself (rate limit, claim conflict), or the application (validation, downstream policy). Without unified observability, each investigation is a hunt across logs, APM and dashboards.

Operators of regulated systems also need an **audit trail** that's separate from generic logs — recording who asked what against the DICT, when, and what came back.

## Decision
Three observability pillars, all correlated by `traceId`:

1. **Traces** — Micrometer Tracing + OpenTelemetry bridge → OTLP → Tempo
2. **Metrics** — Micrometer → Prometheus scrape on `/actuator/prometheus`
3. **Structured logs** — Logback + Logstash JSON encoder → OTLP → Loki

Every log includes `traceId` and `spanId` in the MDC, allowing click-through in Grafana from log → trace → metric.

A single sink, `StructuredAuditLog`, **serves both audit and metrics**: each `AuditEvent` produced by a use case is logged as JSON (compliance trail) and translated into:

- `dict.operation.duration{op,outcome}` — histogram
- `dict.cache.hit{keyType}` — counter
- `dict.gateway.errors{errorClass}` — counter

Centralizing this in a single sink avoids drift between what's audited and what's measured.

PII is **never** in audit/log/span output. The `PixKey.masked()` method produces `02***99`-style values; raw values stay in domain objects only.

## Consequences
### Positive
- One Grafana dashboard for ops investigations.
- Audit and metric instrumentation never disagree (same source).
- Replacing the backend (Datadog, New Relic, etc.) is a Collector reconfiguration, not a code change.

### Negative
- Telemetry volume can be significant — `sampling.probability=1.0` is fine in dev; production should drop to 5–10%.
- The audit sink is an additional component participants must operate (or stream to their existing SIEM).

## Alternatives considered
- **APM vendor-locked (Datadog, New Relic)**: faster turnkey, but couples the code to the vendor and inflates the regulated cost basis. OpenTelemetry is the 2026 industry baseline.
- **Separate audit pipeline (DB table / Kafka topic)**: deferred — log-based audit is sufficient for the v0.1.0 reference; persistent audit will be revisited when a customer integration demands it.
