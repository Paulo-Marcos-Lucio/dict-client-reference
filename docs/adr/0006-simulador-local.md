# ADR 0006 — In-process DICT simulator

## Status
Accepted

## Context
The real DICT is only reachable from registered participants over the SPI private network with an ICP-Brasil client certificate. Developing or testing against it locally is impractical:
- Certificates aren't issued for development purposes.
- The SPI network isn't reachable from outside participant sites.
- Even if it were, hammering the DICT in dev would waste participant rate-limit budget and trigger compliance alerts.

The result, in most participant codebases, is one of:
- **No tests** for the gateway path (only mocks of the gateway interface — useless for integration confidence).
- **Hand-rolled WireMock stubs** scattered across test classes, each maintained separately, drifting from each other.
- **Long mock-test cycles** that catch regressions only when they hit homologation.

## Decision
Ship a **first-class in-process simulator** as part of the project. `DictSimulatorController` implements the same HTTP contract `DictHttpGateway` calls. Mounted under `/dict/v1`, activated by the `simulator` Spring profile.

Key properties:

- **Same JVM**: simulator and client run in the same Spring context for `make run-sim` and integration tests. No second container, no orchestration tax.
- **Real HTTP path**: the cliente serializes, hits the loopback, the simulator deserializes — exercises the full transport, mappers and error handling.
- **Configurable misbehavior**: `SimulatorBehavior` injects failure rate and latency jitter so resilience client-side is exercised on demand.
- **Pre-seeded data**: one entry of each `PixKeyType` is loaded at startup so `requests.http` and IT samples work out of the box.
- **Pluggable**: a participant can replace the simulator with a WireMock instance pointing at recorded BCB responses, or with a remote homologation endpoint, by changing `dict.endpoint.base-url`.

## Consequences
### Positive
- Tests run in CI without secrets, certificates or external dependencies.
- The same code path that runs against the simulator runs against production — no `if simulator` branches in business code.
- New contributors can `git clone && make run-sim && curl ...` and see the system work in under a minute.
- The simulator doubles as **executable documentation** of the contract our client expects.

### Negative
- The simulator must be kept in sync with the manual when the contract evolves. Mitigated by integration tests that fail loudly on contract drift.
- Performance characteristics differ from production (no real network, no real TLS handshake) — load tests need a dedicated environment.
- Simulator code is not production code and must never be deployed there. Enforced by the `@Profile("simulator")` guard and CI sanity check.

## Alternatives considered
- **WireMock with recorded responses**: workable for static fixtures, but harder to evolve and doesn't capture state transitions (claims).
- **Pact contract tests**: complementary — Pact pairs well *with* the simulator (record consumer expectations against simulator, verify against future BCB stub). Tracked as roadmap.
- **No simulator, only homologation**: forces every change through homologation deployment — slow and operationally expensive.
