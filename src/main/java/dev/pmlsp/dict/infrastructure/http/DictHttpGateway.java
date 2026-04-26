package dev.pmlsp.dict.infrastructure.http;

import dev.pmlsp.dict.domain.model.Account;
import dev.pmlsp.dict.domain.model.AccountType;
import dev.pmlsp.dict.domain.model.Claim;
import dev.pmlsp.dict.domain.model.ClaimRole;
import dev.pmlsp.dict.domain.model.ClaimStatus;
import dev.pmlsp.dict.domain.model.ClaimType;
import dev.pmlsp.dict.domain.model.DictEntry;
import dev.pmlsp.dict.domain.model.Ispb;
import dev.pmlsp.dict.domain.model.Owner;
import dev.pmlsp.dict.domain.model.OwnerType;
import dev.pmlsp.dict.domain.model.PixKey;
import dev.pmlsp.dict.domain.model.PixKeyType;
import dev.pmlsp.dict.domain.model.Reason;
import dev.pmlsp.dict.domain.port.out.DictGateway;
import dev.pmlsp.dict.infrastructure.http.dto.HttpDtos;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Calls the DICT (real or simulator) via {@link RestClient}.
 *
 * <p>Resilience is layered through Resilience4j annotations — each operation group binds to
 * a separate instance ({@code dict-lookup}, {@code dict-write}, {@code dict-claim}) so that a
 * spike on one path doesn't starve the others.
 *
 * <p>HTTP failures are funnelled through {@link DictErrorMapper} so that callers always
 * see domain exceptions, never raw Spring/HTTP exceptions.
 */
@Slf4j
public class DictHttpGateway implements DictGateway {

    private static final String INSTANCE_LOOKUP = "dict-lookup";
    private static final String INSTANCE_WRITE = "dict-write";
    private static final String INSTANCE_CLAIM = "dict-claim";

    private final RestClient http;

    public DictHttpGateway(RestClient http) {
        this.http = http;
    }

    @Override
    @CircuitBreaker(name = INSTANCE_LOOKUP)
    @RateLimiter(name = INSTANCE_LOOKUP)
    @Retry(name = INSTANCE_LOOKUP)
    public Optional<DictEntry> lookup(PixKey key, Ispb requesterIspb) {
        try {
            HttpDtos.EntryPayload payload = http.get()
                    .uri("/entries/{type}/{value}", key.type().name(), encode(key.value()))
                    .header("X-Payer-Ispb", requesterIspb.value())
                    .retrieve()
                    .body(HttpDtos.EntryPayload.class);
            return Optional.ofNullable(payload).map(DictHttpGateway::toDomain);
        } catch (HttpStatusCodeException ex) {
            if (ex.getStatusCode().value() == 404) {
                return Optional.empty();
            }
            throw map(ex, key, null);
        } catch (ResourceAccessException ex) {
            throw new dev.pmlsp.dict.domain.exception.DictUnavailableException(
                    "DICT unreachable: " + ex.getMessage(), ex);
        }
    }

    @Override
    @CircuitBreaker(name = INSTANCE_WRITE)
    @Retry(name = INSTANCE_WRITE)
    public DictEntry createEntry(DictEntry entry, Ispb requesterIspb) {
        try {
            HttpDtos.EntryPayload created = http.post()
                    .uri("/entries")
                    .header("X-Participant-Ispb", requesterIspb.value())
                    .body(toCreateRequest(entry))
                    .retrieve()
                    .body(HttpDtos.EntryPayload.class);
            return toDomain(created);
        } catch (HttpStatusCodeException ex) {
            throw map(ex, entry.key(), null);
        }
    }

    @Override
    @CircuitBreaker(name = INSTANCE_WRITE)
    @Retry(name = INSTANCE_WRITE)
    public void deleteEntry(PixKey key, Reason reason, Ispb requesterIspb) {
        try {
            http.method(org.springframework.http.HttpMethod.DELETE)
                    .uri("/entries/{type}/{value}", key.type().name(), encode(key.value()))
                    .header("X-Participant-Ispb", requesterIspb.value())
                    .body(new HttpDtos.DeleteEntryRequest(reason.name()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpStatusCodeException ex) {
            throw map(ex, key, null);
        }
    }

    @Override
    @CircuitBreaker(name = INSTANCE_CLAIM)
    @Retry(name = INSTANCE_CLAIM)
    public Claim startClaim(ClaimType type, PixKey key, Account claimerAccount, Owner claimerOwner, Ispb claimerIspb) {
        try {
            HttpDtos.StartClaimRequest body = new HttpDtos.StartClaimRequest(
                    type.name(),
                    new HttpDtos.KeyPayload(key.type().name(), key.value()),
                    toAccountPayload(claimerAccount),
                    toOwnerPayload(claimerOwner));
            HttpDtos.ClaimPayload created = http.post()
                    .uri("/claims")
                    .header("X-Participant-Ispb", claimerIspb.value())
                    .body(body)
                    .retrieve()
                    .body(HttpDtos.ClaimPayload.class);
            return toDomain(created);
        } catch (HttpStatusCodeException ex) {
            throw map(ex, key, null);
        }
    }

    @Override
    @CircuitBreaker(name = INSTANCE_CLAIM)
    @Retry(name = INSTANCE_CLAIM)
    public Claim acknowledgeClaim(UUID claimId, Ispb donorIspb) {
        return claimAction(claimId, donorIspb, "acknowledge", null);
    }

    @Override
    @CircuitBreaker(name = INSTANCE_CLAIM)
    @Retry(name = INSTANCE_CLAIM)
    public Claim completeClaim(UUID claimId, Ispb requesterIspb) {
        return claimAction(claimId, requesterIspb, "complete", null);
    }

    @Override
    @CircuitBreaker(name = INSTANCE_CLAIM)
    @Retry(name = INSTANCE_CLAIM)
    public Claim cancelClaim(UUID claimId, Ispb requesterIspb, Reason reason) {
        return claimAction(claimId, requesterIspb, "cancel", new HttpDtos.CancelClaimRequest(reason.name()));
    }

    @Override
    @CircuitBreaker(name = INSTANCE_LOOKUP)
    @RateLimiter(name = INSTANCE_LOOKUP)
    @Retry(name = INSTANCE_LOOKUP)
    public List<Claim> listClaims(ClaimRole role, Ispb ispb) {
        try {
            HttpDtos.ClaimPayload[] result = http.get()
                    .uri(uriBuilder -> uriBuilder.path("/claims")
                            .queryParam("role", role.name().toLowerCase())
                            .queryParam("ispb", ispb.value())
                            .build())
                    .retrieve()
                    .body(HttpDtos.ClaimPayload[].class);
            if (result == null) return List.of();
            return java.util.Arrays.stream(result).map(DictHttpGateway::toDomain).toList();
        } catch (HttpStatusCodeException ex) {
            throw map(ex, null, null);
        }
    }

    private Claim claimAction(UUID claimId, Ispb requesterIspb, String action, Object body) {
        try {
            HttpDtos.ClaimPayload result = http.post()
                    .uri("/claims/{id}/{action}", claimId, action)
                    .header("X-Participant-Ispb", requesterIspb.value())
                    .body(body == null ? "" : body)
                    .retrieve()
                    .body(HttpDtos.ClaimPayload.class);
            return toDomain(result);
        } catch (HttpStatusCodeException ex) {
            throw map(ex, null, claimId);
        }
    }

    private static dev.pmlsp.dict.domain.exception.DictException map(
            HttpStatusCodeException ex, PixKey key, UUID claimId) {
        HttpStatusCode status = ex.getStatusCode();
        HttpDtos.ProblemPayload problem = parseProblem(ex);
        return DictErrorMapper.toDomain(status, ex.getResponseHeaders(), problem, key, claimId);
    }

    private static HttpDtos.ProblemPayload parseProblem(HttpStatusCodeException ex) {
        try {
            return ex.getResponseBodyAs(HttpDtos.ProblemPayload.class);
        } catch (Exception ignored) {
            return null;
        }
    }

    // ---------- mapping helpers ----------

    private static String encode(String raw) {
        return URLEncoder.encode(raw, StandardCharsets.UTF_8);
    }

    private static HttpDtos.CreateEntryRequest toCreateRequest(DictEntry entry) {
        return new HttpDtos.CreateEntryRequest(
                new HttpDtos.KeyPayload(entry.key().type().name(), entry.key().value()),
                toAccountPayload(entry.account()),
                toOwnerPayload(entry.owner()));
    }

    private static HttpDtos.AccountPayload toAccountPayload(Account a) {
        return new HttpDtos.AccountPayload(a.ispb().value(), a.branch(), a.number(), a.type().name());
    }

    private static HttpDtos.OwnerPayload toOwnerPayload(Owner o) {
        return new HttpDtos.OwnerPayload(o.type().name(), o.document(), o.name(), o.tradeName());
    }

    private static DictEntry toDomain(HttpDtos.EntryPayload p) {
        return new DictEntry(
                new PixKey(PixKeyType.valueOf(p.key().type()), p.key().value()),
                toAccount(p.account()),
                toOwner(p.owner()),
                p.createdAt() == null ? Instant.now() : p.createdAt(),
                p.keyOwnershipDate() == null ? Instant.now() : p.keyOwnershipDate(),
                p.openClaimType() == null ? null : ClaimType.valueOf(p.openClaimType()));
    }

    private static Claim toDomain(HttpDtos.ClaimPayload p) {
        return new Claim(
                p.claimId(),
                ClaimType.valueOf(p.type()),
                ClaimStatus.valueOf(p.status()),
                new PixKey(PixKeyType.valueOf(p.key().type()), p.key().value()),
                toAccount(p.claimerAccount()),
                toOwner(p.claimerOwner()),
                Ispb.of(p.claimerIspb()),
                Ispb.of(p.donorIspb()),
                p.requestedAt(),
                p.resolutionDeadline(),
                p.completedAt(),
                p.cancellationReason() == null ? null : Reason.valueOf(p.cancellationReason()));
    }

    private static Account toAccount(HttpDtos.AccountPayload p) {
        return new Account(Ispb.of(p.ispb()), p.branch(), p.number(), AccountType.valueOf(p.type()));
    }

    private static Owner toOwner(HttpDtos.OwnerPayload p) {
        return new Owner(OwnerType.valueOf(p.type()), p.document(), p.name(), p.tradeName());
    }
}
