# BCB DICT — Compliance mapping

This document maps each rule from the *Manual de Operações do DICT* (BCB) to the file/class in this project that enforces it. **Always cross-check against the manual version current at the time of your homologation** — values evolve.

> ⚠️ This document references the manual at a specific point in time. Values below are illustrative of how compliance should be wired into code; verify each line against the version of the manual provided to your participant before going live.

## 1. Cache TTL by key type

| Rule (manual) | Implementation |
|---|---|
| TTL máximo de cache para chave CPF: 60s | [`CacheTtlPolicy.MAX_CPF`](../../src/main/java/dev/pmlsp/dict/domain/policy/CacheTtlPolicy.java) |
| TTL máximo de cache para chave CNPJ: 300s | `CacheTtlPolicy.MAX_CNPJ` |
| TTL máximo de cache para chave EMAIL: 300s | `CacheTtlPolicy.MAX_EMAIL` |
| TTL máximo de cache para chave PHONE: 300s | `CacheTtlPolicy.MAX_PHONE` |
| TTL máximo de cache para chave EVP: 60s | `CacheTtlPolicy.MAX_EVP` |
| Chave com claim em aberto não deve ser cacheada | `RegulatoryCacheTtlPolicy.ttlFor()` returns `ZERO` when `entry.hasOpenClaim()` |
| Operador não pode configurar TTL acima do máximo | `RegulatoryCacheTtlPolicy` clamps and emits WARN log |

## 2. Identificação do participante

| Rule | Implementation |
|---|---|
| ISPB sempre 8 dígitos numéricos | [`Ispb`](../../src/main/java/dev/pmlsp/dict/domain/model/Ispb.java) record (validation) |
| Header `X-Participant-Ispb` em writes / claims | [`DictHttpGateway`](../../src/main/java/dev/pmlsp/dict/infrastructure/http/DictHttpGateway.java) |
| Header `X-Payer-Ispb` em lookup | `DictHttpGateway.lookup` |

## 3. Claim windows (resolution deadline)

| Rule | Implementation |
|---|---|
| Janela de portability: 7 dias | [`ClaimType.PORTABILITY.resolutionWindow()`](../../src/main/java/dev/pmlsp/dict/domain/model/ClaimType.java) |
| Janela de ownership: 30 dias | `ClaimType.OWNERSHIP.resolutionWindow()` |
| Deadline calculado no momento da abertura | `Claim.open(...)` constructor |

## 4. State machine de claim

| Rule | Implementation |
|---|---|
| Transições válidas: `OPEN → WAITING_RESOLUTION → CONFIRMED → COMPLETED` | [`Claim.acknowledge() / confirm() / complete()`](../../src/main/java/dev/pmlsp/dict/domain/model/Claim.java) |
| Cancellation possível apenas em estados não-terminais | `Claim.cancel(reason)` throws if status is terminal |
| Estados terminais: `COMPLETED`, `CANCELLED` | `ClaimStatus.isTerminal()` |
| Claimer e donor devem ser ISPBs distintos | `Claim` constructor invariant |

## 5. PII em logs/audit

| Rule | Implementation |
|---|---|
| Chaves Pix devem ser mascaradas em logs | [`PixKey.masked()`](../../src/main/java/dev/pmlsp/dict/domain/model/PixKey.java) |
| Audit log deve registrar chave mascarada, ISPB do solicitante, operação, outcome, duração | [`AuditEvent`](../../src/main/java/dev/pmlsp/dict/domain/port/out/AuditEvent.java) + [`StructuredAuditLog`](../../src/main/java/dev/pmlsp/dict/infrastructure/audit/StructuredAuditLog.java) |

## 6. mTLS

| Rule | Implementation |
|---|---|
| Conexão ao DICT deve usar mTLS com cert ICP-Brasil | [ADR 0002](../adr/0002-mtls-icp-brasil.md), `dict.mtls.bundle-name` → Spring `SslBundle` |
| Chave privada nunca commitada | `.gitignore` blocks `*.p12 *.pfx *.jks *.key *.pem` |

## 7. Códigos de motivo (Reason)

| Rule (códigos aceitos pelo DICT) | Implementation |
|---|---|
| `USER_REQUESTED`, `ACCOUNT_CLOSURE`, `BRANCH_TRANSFER`, `RECONCILIATION`, `FRAUD`, `MASS_OPERATION`, `REJECTED_BY_PSP` | [`Reason` enum](../../src/main/java/dev/pmlsp/dict/domain/model/Reason.java) |

---

When the manual changes, update **both** the constant/code and the row in this table in the same PR. Reviewers should reject changes to constants that don't update this map.
