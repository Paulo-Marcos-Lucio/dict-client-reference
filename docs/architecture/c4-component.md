# C4 — Level 3: Component (zoom into use cases + adapters)

```mermaid
flowchart TB
    subgraph Domain["domain (pure)"]
        Model[("Models<br/>PixKey · Account · Owner<br/>DictEntry · Claim · Reason")]
        Policy[CacheTtlPolicy<br/>interface]
        PortIn[(Use case ports<br/>LookupKeyUseCase ...)]
        PortOut[(Output ports<br/>DictGateway · DictEntryCache · AuditLog)]
    end

    subgraph Application["application"]
        Lookup[LookupKeyService]
        CreateE[CreateEntryService]
        DeleteE[DeleteEntryService]
        StartC[StartClaimService]
        AckC[AcknowledgeClaimService]
        CompleteC[CompleteClaimService]
        CancelC[CancelClaimService]
        ListC[ListClaimsService]
    end

    subgraph Infrastructure["infrastructure"]
        HttpGw[DictHttpGateway<br/>+Resilience4j]
        ClientFac[DictHttpClientFactory<br/>RestClient + SslBundle]
        ErrMap[DictErrorMapper]
        Caffeine[CaffeineDictEntryCache]
        TtlImpl[RegulatoryCacheTtlPolicy]
        Audit[StructuredAuditLog]
        Sim[DictSimulatorController<br/>+InMemoryDictStore<br/>+SimulatorBehavior]
    end

    subgraph Adapter["adapter / web"]
        Controller[DictFacadeController]
        ExHandler[GlobalExceptionHandler]
    end

    Controller --> PortIn
    PortIn -.implementam.-> Lookup
    PortIn -.implementam.-> CreateE
    PortIn -.implementam.-> DeleteE
    PortIn -.implementam.-> StartC
    PortIn -.implementam.-> AckC
    PortIn -.implementam.-> CompleteC
    PortIn -.implementam.-> CancelC
    PortIn -.implementam.-> ListC

    Lookup --> PortOut
    CreateE --> PortOut
    DeleteE --> PortOut
    StartC --> PortOut
    AckC --> PortOut
    CompleteC --> PortOut
    CancelC --> PortOut
    ListC --> PortOut
    Lookup --> Policy

    PortOut -.impl.-> HttpGw
    PortOut -.impl.-> Caffeine
    PortOut -.impl.-> Audit
    Policy -.impl.-> TtlImpl

    HttpGw --> ClientFac
    HttpGw --> ErrMap
    HttpGw -.in tests/dev.-> Sim

    classDef domain fill:#fff5d6,stroke:#b08800,color:#000
    classDef app fill:#dff5e1,stroke:#2e7d32,color:#000
    classDef adapter fill:#e3f0ff,stroke:#1565c0,color:#000
    classDef infra fill:#fde2e2,stroke:#b71c1c,color:#000
    class Model,Policy,PortIn,PortOut domain
    class Lookup,CreateE,DeleteE,StartC,AckC,CompleteC,CancelC,ListC app
    class Controller,ExHandler adapter
    class HttpGw,ClientFac,ErrMap,Caffeine,TtlImpl,Audit,Sim infra
```

## Read order

1. **Start with `domain/`** — models and ports define what's possible.
2. **Read a use case** (e.g. `LookupKeyService`) — see how it composes ports and how it audits each step.
3. **Read the gateway** (`DictHttpGateway`) to understand the HTTP contract, error mapping, and Resilience4j placement.
4. **Read the simulator** (`DictSimulatorController`) — same contract, no transport security, deterministic behavior.

Boundaries are enforced by `HexagonalArchitectureTest` — domain has zero Spring/Jakarta dependency, application depends only on domain, adapter is a leaf, infrastructure is the only place that knows about HTTP, Caffeine, Logstash etc.
