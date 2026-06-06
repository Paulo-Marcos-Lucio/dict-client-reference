# Changelog

Todas as mudanças relevantes deste projeto são documentadas neste arquivo.

O formato segue [Keep a Changelog](https://keepachangelog.com/pt-BR/1.1.0/) e o versionamento segue [Semantic Versioning](https://semver.org/lang/pt-BR/).

## [Unreleased]

### Changed — Manutenção junho/2026
- **Migração para Spring Boot 4.0.6** (de 3.4.1) — Spring Framework 7, Jakarta EE 11, Tomcat 11, Jackson 3 como default
  - `DictHttpClientFactory` migrado para a nova API de factory HTTP do Boot 4: `ClientHttpRequestFactoryBuilder.jdk().build(HttpClientSettings...)` (os tipos saíram de `org.springframework.boot.web.client` para o módulo `spring-boot-http-client`, pacote `org.springframework.boot.http.client`)
  - Adicionada dependência explícita `spring-boot-http-client` (não vem transitiva pelo `starter-web` no Boot 4)
  - `GlobalExceptionHandler` usa `HttpStatus.UNPROCESSABLE_CONTENT` (422) no lugar de `UNPROCESSABLE_ENTITY`, deprecado no Framework 7
- Resilience4j 2.2.0 → **2.4.0**, trocando `resilience4j-spring-boot3` por `resilience4j-spring-boot4` (compatível com Framework 7)
- springdoc-openapi 2.7.0 → **3.0.3** (linha 3.x exigida pelo Boot 4)
- logstash-logback-encoder 8.0 → **9.0**
- lombok 1.18.36 → **1.18.46**
- archunit 1.3.0 → **1.4.2**, wiremock 3.10.0 → **3.13.2**
- jacoco-maven-plugin 0.8.12 → **0.8.15**
- GitHub Actions atualizadas (grupo Dependabot `actions`, 11 updates: checkout v6, setup-java v5, upload/download-artifact v7/v8, codeql-action v4, etc.)
- Removidas as properties mortas `opentelemetry.version` e `testcontainers.version` do `pom.xml` (não eram referenciadas; versões gerenciadas pelo BOM do Boot)

### Fixed — Manutenção junho/2026
- `config/tempo/tempo.yml`: removido o bloco `metadata_slo` que fazia o Tempo 2.6.1 local crashar na subida

[Unreleased]: https://github.com/Paulo-Marcos-Lucio/dict-client-reference/compare/v0.1.0...HEAD

## [0.1.0] - 2026-04-26

Primeira release pública. Cliente Java de referência para o DICT (Diretório de Identificadores de Contas Transacionais) do Banco Central, com simulador local rodando junto.

### Added — Cliente DICT
- Operações cobertas: `lookup` de chave, `create`/`delete` de entry, `startPortability`, `startClaim`, `complete`/`cancel` de claim, `list` de claims abertas
- Modelo de domínio puro (`PixKey`, `Account`, `Owner`, `DictEntry`, `Claim`) — sem Spring, sem Jakarta, validação de cada tipo de chave conforme manual BCB
- Portas in/out hexagonais (`DictGateway`, `DictEntryCache`, `AuditLog`)
- Use cases em `application/` orquestrando cache → gateway → audit, sem dependência direta da infraestrutura

### Added — Infraestrutura
- HTTP client baseado em Spring `RestClient` com **mTLS configurável** via `SslBundle` (truststore ICP-Brasil + keystore do participante)
- `CacheTtlPolicy` aplica TTL conforme manual DICT — chaves de pessoa natural cacheadas por menos tempo que chaves de pessoa jurídica, e zero cache para chaves com claim em aberto
- `CaffeineDictEntryCache` in-process com expiração por entrada
- Resilience4j por operação: `dict-lookup` (rate limiter agressivo), `dict-write` (circuit breaker + retry exponencial), `dict-claim` (retry conservador)
- Audit log estruturado JSON com PII mascarada (cada chave Pix sai com `02***99`, nunca em claro)
- Observabilidade end-to-end: spans OTel com tags `dict.operation`, `dict.key.type`, `dict.ispb`; métricas Micrometer (`dict.operation.duration`, `dict.cache.hit`, `dict.cache.miss`)
- Mapeamento `DictErrorMapper` traduz códigos HTTP/erro do BCB para exceções de domínio (`KeyNotFoundException`, `DuplicateKeyException`, `RateLimitedException`, etc.)

### Added — Simulador local
- `DictSimulatorController` (ativo no profile `simulator`) implementa o contrato HTTP do DICT em memória
- `InMemoryDictStore` com chaves pré-populadas para os 5 tipos (CPF, CNPJ, EMAIL, PHONE, EVP)
- `SimulatorBehavior` configurável: taxa de erro, latência adicional — permite testar resiliência sem chave do BCB
- Roda no mesmo Spring context da app via `@Profile("simulator")`, ou destacado em outra porta para fechar o loop end-to-end

### Added — Adapter web (demo)
- `DictFacadeController` expõe REST facade sobre os use cases — usado pelo `requests.http` e por testes de integração
- `GlobalExceptionHandler` traduz exceções de domínio em respostas HTTP RFC 7807 (Problem Details)
- OpenAPI 3 autogerado via SpringDoc

### Added — Tooling
- `ci.yml` paralelo: build, unit tests, integration tests, ArchUnit, Semgrep SAST, Trivy image scan
- `codeql.yml` (semanal + on-PR), `dependency-review.yml`, `release.yml` (tag-driven, OCI image em GHCR + GitHub Release com SBOM)
- Dependabot agrupado por categoria (Spring Boot, Testcontainers, observability, resilience, GitHub Actions, Docker)
- 6 ADRs documentando: hexagonal, mTLS ICP-Brasil, cache TTL regulatório, estratégia de resiliência, observabilidade correlacionada, simulador local
- C4 (context, container, component) e fluxos de operações em `docs/`
- `compose.yaml` com toda a stack de observabilidade (otel + prometheus + tempo + loki + grafana)
- Templates de issue/PR, `CODEOWNERS`, `FUNDING.yml`, `SECURITY.md`, `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md`, `CLAUDE.md`

[0.1.0]: https://github.com/Paulo-Marcos-Lucio/dict-client-reference/releases/tag/v0.1.0
