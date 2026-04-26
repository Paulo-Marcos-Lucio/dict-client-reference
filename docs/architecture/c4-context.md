# C4 — Level 1: Context

```mermaid
C4Context
title System Context — DICT Client Reference

Person(participant_dev, "Participant developer", "Engineer integrating their fintech / bank into the Pix ecosystem")
System(client, "DICT Client (this project)", "Java library + demo facade that calls the BCB DICT")

System_Ext(dict, "Banco Central — DICT", "Diretório de Identificadores de Contas Transacionais — official key-to-account directory")
System_Ext(simulator, "Local DICT Simulator", "In-process implementation of the DICT contract for development and testing (this project, profile=simulator)")
System_Ext(payment_app, "Payer / Merchant application", "Calls the participant to initiate a Pix payment, which requires a DICT lookup")

Rel(participant_dev, client, "Embeds as library or runs facade demo", "Maven dependency / HTTP")
Rel(client, dict, "Lookup, create, delete, claim", "HTTPS + mTLS (ICP-Brasil) + JSON")
Rel(client, simulator, "Same contract, no mTLS", "HTTP + JSON (loopback)")
Rel(payment_app, client, "Resolves chave Pix to account before iniciação", "Use case API or HTTP facade")
```

## Macro flow

1. Payment app needs to initiate a Pix to a key — calls the participant's gateway, which uses this client to resolve the key into an account.
2. Client checks local cache. On hit (and key has no claim in flight), returns immediately.
3. On miss, client makes an HTTPS request to the DICT (mTLS in production, plain HTTP to the simulator in dev/test).
4. Response is mapped to domain types, audited, optionally cached, returned.
5. Same client also issues writes (create/delete) and claim operations (portability/ownership) on demand.
