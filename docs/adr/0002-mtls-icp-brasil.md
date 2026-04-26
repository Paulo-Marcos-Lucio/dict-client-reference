# ADR 0002 — mTLS via SslBundle with ICP-Brasil chain

## Status
Accepted

## Context
The DICT in production accepts only mTLS connections from registered participants, with the client certificate issued by the **ICP-Brasil** (Infraestrutura de Chaves Públicas Brasileira) chain. Each request must be authenticated by a private key held exclusively by the participant institution.

Common pitfalls in real-world implementations:
- Hardcoding the keystore path / password in code or in `application.yml`.
- Mixing the participant's keystore with the JVM-wide truststore.
- Reloading the SSLContext per request (huge perf hit).
- Trusting any cert that chains to a public CA (defeats the purpose of mTLS for Pix).

## Decision
Use Spring Boot 3's **`SslBundle`** abstraction, configured via `spring.ssl.bundle.jks.<name>`:

```yaml
spring:
  ssl:
    bundle:
      jks:
        dict-prod:
          key:
            alias: ${DICT_KEY_ALIAS}
          keystore:
            location: file:${DICT_KEYSTORE_PATH}
            password: ${DICT_KEYSTORE_PASSWORD}
            type: PKCS12
          truststore:
            location: file:${DICT_TRUSTSTORE_PATH}
            password: ${DICT_TRUSTSTORE_PASSWORD}
            type: JKS
```

The application then references the bundle by name:

```yaml
dict:
  mtls:
    enabled: true
    bundle-name: dict-prod
```

`DictHttpClientFactory` resolves the bundle from `SslBundles` and wires it into the `RestClient` via `ClientHttpRequestFactorySettings.withSslBundle(...)`. The HTTP client is built once at startup; no per-request work.

In `local` and `simulator` profiles, mTLS is disabled and the gateway uses HTTP plain against the in-process simulator.

## Consequences
### Positive
- Zero crypto code in the application — Spring Boot resolves `SSLContext` from `SslBundle`.
- Truststore and keystore paths/passwords come from env vars or secret manager — never committed.
- Bundle reload via Spring's hot-reload mechanism (when supported by the bundle backend) — useful for cert rotation without downtime.
- Same code path runs against simulator (mTLS off) and production (mTLS on) — only YAML changes.

### Negative
- The participant must obtain and renew an ICP-Brasil certificate (out of scope for this project).
- Certificate expiry monitoring must be added by the operator (e.g. a scheduled task that reads the keystore and alerts on near-expiry).

## Alternatives considered
- **Manual SSLContext construction**: more code, more places to leak resources, no benefit over `SslBundle`.
- **Vault sidecar / Hashicorp PKI**: deferred — useful in mature deployments where dynamic cert rotation is required, but overkill for a reference implementation. Tracked in roadmap.
- **Mutual TLS at ingress (e.g. nginx) instead of in-app**: viable in Kubernetes setups; this project supports both — when the ingress terminates mTLS, set `dict.mtls.enabled=false` and harden the network path between ingress and app.
