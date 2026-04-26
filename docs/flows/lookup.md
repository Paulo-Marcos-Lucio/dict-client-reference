# Flow — Lookup de chave Pix

```mermaid
sequenceDiagram
    autonumber
    participant Caller as Use case caller<br/>(facade ou outra app)
    participant UC as LookupKeyService
    participant C as DictEntryCache (Caffeine)
    participant P as CacheTtlPolicy
    participant G as DictHttpGateway
    participant D as DICT (real ou simulator)
    participant A as StructuredAuditLog

    Caller->>UC: lookup(LookupQuery{key, payerIspb})
    UC->>C: get(key)
    alt cache hit
        C-->>UC: Optional.of(entry)
        UC->>A: record(CACHE_HIT)
        UC-->>Caller: DictEntry
    else cache miss
        C-->>UC: Optional.empty()
        UC->>G: lookup(key, payerIspb) (Resilience4j: lookup CB+RL+Retry)
        G->>D: GET /entries/{type}/{value}
        alt 200 OK
            D-->>G: EntryPayload
            G-->>UC: Optional.of(entry)
            UC->>P: ttlFor(entry)
            P-->>UC: Duration (zero se claim aberto)
            opt ttl > 0
                UC->>C: put(key, entry, ttl)
            end
            UC->>A: record(SUCCESS)
            UC-->>Caller: DictEntry
        else 404
            G-->>UC: Optional.empty()
            UC->>A: record(NOT_FOUND)
            UC-->>Caller: throw KeyNotFoundException
        else 5xx / timeout
            G-->>UC: throw DictUnavailableException
            UC->>A: record(ERROR)
            UC-->>Caller: throw DictUnavailableException
        end
    end
```

## Garantias

- **Cache hit nunca leva ao DICT** — minimiza latência e quota.
- **Open claim entries nunca entram no cache** — `CacheTtlPolicy` retorna zero, e `LookupKeyService` ignora valores zero.
- **Erros transitórios (5xx, timeout) entram no `dict-lookup` retry/CB** — sem afetar o operador.
- **Erros determinísticos (404, 422) sobem direto** — não retentam.
