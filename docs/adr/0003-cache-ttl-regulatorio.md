# ADR 0003 — Cache TTL aware of BCB regulatory limits

## Status
Accepted

## Context
The DICT operations manual imposes **maximum TTLs** that participants may use to cache lookup results. The rationale is to bound how stale account information can be in the Pix payment chain — a key that's been ported or revoked must converge across all participants quickly.

Two failure modes are common:
1. Participants over-cache to avoid rate limits and end up referencing stale ownership.
2. Participants under-cache and get throttled by the DICT, hurting end-user latency.

Additionally, **a key with an open claim must not be cached at all** — pricing, ownership and account routing are all in flux during a claim window.

## Decision
Introduce a **pure-domain `CacheTtlPolicy` interface** that exposes the BCB maxima as constants and demands implementations clamp configured TTLs to those maxima:

```java
public interface CacheTtlPolicy {
    Duration ZERO = Duration.ZERO;
    Duration MAX_CPF   = Duration.ofSeconds(60);
    Duration MAX_CNPJ  = Duration.ofSeconds(300);
    Duration MAX_EMAIL = Duration.ofSeconds(300);
    Duration MAX_PHONE = Duration.ofSeconds(300);
    Duration MAX_EVP   = Duration.ofSeconds(60);

    Duration ttlFor(DictEntry entry);
}
```

The default implementation, `RegulatoryCacheTtlPolicy`:

1. Returns `ZERO` if `entry.hasOpenClaim()`.
2. Reads the configured TTL for the key type from `dict.cache.ttl.*`.
3. Returns `min(configured, regulatoryMax)` — emits a `WARN` log when it has to clamp, so operators know their config is incompatible with the manual.

`LookupKeyService` uses the policy result to decide whether to cache. `CaffeineDictEntryCache` honors per-entry TTL via Caffeine's `Expiry` mechanism.

## Consequences
### Positive
- Operator can't over-cache by accident — the clamp is a domain invariant, not just a yaml comment.
- Open-claim entries are guaranteed never to enter cache (audit-friendly).
- Per-entry TTL means each cached key honors the right regulatory ceiling for its type.

### Negative
- Cache hit rate is lower than a fixed 5-minute cache (intentional).
- Caffeine's `Expiry` has slightly more overhead than a fixed `expireAfterWrite` policy. Acceptable for the workload.
- The hardcoded constants must be kept in sync with the BCB manual version. Tracked as part of compliance review (see `docs/compliance/bcb-mapping.md`).

## Alternatives considered
- **Single global TTL**: simpler, but violates BCB rule for `EVP` (60s max) when set above that, and wastes cache for `CNPJ` (could be 300s).
- **Pull TTL from a `Cache-Control` header on the DICT response**: would be ideal, but the DICT does not currently emit such header. Tracked as a roadmap item to revisit.
- **No client-side cache at all**: violates the SLA expectations of payment iniciation paths and would saturate the DICT rate budget.
