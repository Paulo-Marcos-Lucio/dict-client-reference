# C4 — Level 2: Containers

```mermaid
C4Container
title Containers — DICT Client Reference

Person(payer_app, "Payer / merchant app")

System_Boundary(host, "Participant deployment") {
    Container(facade, "Demo Facade", "Spring MVC + RestController", "Optional REST entry-point — useful for IT and quick ops checks")
    Container(usecases, "Use Cases", "Spring beans, pure orchestration", "LookupKey, CreateEntry, DeleteEntry, *Claim")
    Container(http_gateway, "DictHttpGateway", "Spring RestClient + Resilience4j", "Talks to DICT or simulator over HTTPS+mTLS / HTTP")
    Container(cache, "DictEntryCache", "Caffeine in-process", "TTL clamped by CacheTtlPolicy; zero TTL on open claims")
    Container(audit_sink, "StructuredAuditLog", "Logback JSON + Micrometer", "Both compliance log and metric source")
    Container(simulator, "DictSimulator", "Spring MVC controller (profile=simulator)", "In-process implementation of the DICT contract")
}

System_Ext(dict, "BCB DICT", "Production directory")
System_Ext(otel, "OTel Collector → Tempo / Prometheus / Loki", "Observability backbone")

Rel(payer_app, facade, "HTTP requests")
Rel(facade, usecases, "Java calls")
Rel(usecases, cache, "get / put / invalidate")
Rel(usecases, http_gateway, "lookup, create, claim ...")
Rel(usecases, audit_sink, "record(event)")
Rel(http_gateway, dict, "HTTPS + mTLS", "production")
Rel(http_gateway, simulator, "HTTP", "dev / test")
Rel(audit_sink, otel, "JSON logs / metrics", "OTLP")
```

## Notes

- The **simulator** is just another container in the same JVM — the gateway sees the same HTTP contract, the network is loopback. This is what makes integration tests credible without external dependencies.
- The **audit sink** doubles as the metric source so the metric and audit views never disagree.
- The **cache** is per-instance (Caffeine). Multi-replica deployments can plug a Redis-backed implementation of `DictEntryCache` without touching the use cases — that's the point of the hexagonal split.
