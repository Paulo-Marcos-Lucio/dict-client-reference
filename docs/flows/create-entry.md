# Flow — Criar entry (registrar chave Pix)

```mermaid
sequenceDiagram
    autonumber
    participant U as Cliente final
    participant App as App do banco (participant)
    participant CC as DictClient
    participant DICT as BCB / DICT

    U->>App: registra chave Pix (ex: e-mail)
    App->>CC: CreateEntryUseCase.create({key, account, owner, requesterIspb})
    Note right of CC: domain validations:<br/>PixKey value matches type pattern<br/>Account ispb / branch / number<br/>Owner document matches type
    CC->>DICT: POST /entries (Resilience4j: write CB+Retry)
    alt 201 Created
        DICT-->>CC: EntryPayload (createdAt, keyOwnershipDate)
        CC->>CC: cache.invalidate(key) (defensive)
        CC->>CC: audit(SUCCESS)
        CC-->>App: DictEntry
    else 409 DUPLICATE_KEY
        DICT-->>CC: ProblemPayload{code: DUPLICATE_KEY}
        CC->>CC: audit(ERROR, DuplicateKeyException)
        CC-->>App: throw DuplicateKeyException
    else 422 Policy Violation
        DICT-->>CC: ProblemPayload{code: ...}
        CC-->>App: throw PolicyViolationException
    else 5xx (transient)
        DICT-->>CC: 5xx
        Note over CC,DICT: Resilience4j write retry: 2 attempts, exp backoff
        CC-->>App: throw DictUnavailableException
    end
```

## Garantias

- **Write não retenta erros 4xx** — apenas 5xx / timeout. 4xx é sinal de input inválido ou conflito real, não falha transitória.
- **Cache é invalidado** mesmo em writes bem-sucedidos — garante que próximos lookups peguem o estado fresco do DICT (createdAt, keyOwnershipDate corretos).
- **Cliente nunca tenta unique-check local** — a unicidade é responsabilidade do DICT (única fonte da verdade).
