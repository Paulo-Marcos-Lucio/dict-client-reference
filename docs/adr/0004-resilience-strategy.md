# ADR 0004 — Resilience strategy per operation group

## Status
Accepted

## Context
The DICT exposes operations with very different traffic profiles and idempotency guarantees:

- **Lookup** (`GET /entries/...`): high-frequency, idempotent, cache-friendly. Throttling here directly hurts payment iniciation latency.
- **Write** (`POST /entries`, `DELETE /entries/...`): low-frequency, conditionally idempotent (DICT enforces uniqueness). Failed writes can have side effects (orphaned reservations).
- **Claim** (`POST /claims`, `/acknowledge`, `/complete`, `/cancel`): medium-frequency, multi-step state machine across days. Aggressive retry could fire the same claim twice.

A one-size-fits-all retry/circuit-breaker would either over-protect lookups or under-protect writes.

## Decision
Define **three Resilience4j instance groups**, applied via annotations on `DictHttpGateway`:

| Group | CB window | Retry attempts | Rate limiter | Notes |
|---|---|---|---|---|
| `dict-lookup` | 50 calls, 50% threshold | 3 with exp backoff (200ms base, x2) | 50 req/s, 100ms wait | Active rate-limit to stay below DICT participant quota |
| `dict-write` | 20 calls, 30% threshold | 2 with exp backoff (300ms base, x2) | — | Stricter CB threshold; fewer retries to avoid duplicates |
| `dict-claim` | 20 calls, 50% threshold | 2 with exp backoff (500ms base) | — | Conservative — claim ops are rare and side-effectful |

All groups retry **only** on `DictUnavailableException` (5xx / connection failures). They never retry on `RateLimitedException`, `PolicyViolationException`, `DuplicateKeyException` etc. — those are deterministic and need a behavior change, not another attempt.

Configuration lives in `application.yml` under `resilience4j.*`, so operators can tune without rebuilding.

## Consequences
### Positive
- Lookup spikes don't starve writes (separate CBs).
- A 5xx storm trips the CB fast on the affected group, sheds load, lets the DICT recover.
- Rate limiter on lookup keeps the participant quota honest.

### Negative
- Three sets of metrics to watch (each group emits its own CB / retry / RL metrics).
- Annotation-based binding means each method on the gateway must be explicitly tagged — easy to miss when adding new ops. Mitigated by `DictGateway` interface review.

## Alternatives considered
- **Unified resilience layer wrapping the use cases**: simpler, but couples retry policy to business intent rather than transport. Same business intent might want different retry depending on whether DICT or another adapter is being called.
- **Spring Retry / Spring Cloud Circuit Breaker abstraction**: viable; chosen Resilience4j directly because of its richer metric surface and bulkhead support (next-iteration use).
- **Sidecar (Envoy) for resilience**: out of scope; adds operational complexity disproportionate to a reference implementation.
