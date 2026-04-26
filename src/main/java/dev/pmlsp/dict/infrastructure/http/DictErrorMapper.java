package dev.pmlsp.dict.infrastructure.http;

import dev.pmlsp.dict.domain.exception.ClaimConflictException;
import dev.pmlsp.dict.domain.exception.ClaimNotFoundException;
import dev.pmlsp.dict.domain.exception.DictException;
import dev.pmlsp.dict.domain.exception.DictUnavailableException;
import dev.pmlsp.dict.domain.exception.DuplicateKeyException;
import dev.pmlsp.dict.domain.exception.KeyNotFoundException;
import dev.pmlsp.dict.domain.exception.PolicyViolationException;
import dev.pmlsp.dict.domain.exception.RateLimitedException;
import dev.pmlsp.dict.domain.model.PixKey;
import dev.pmlsp.dict.infrastructure.http.dto.HttpDtos.ProblemPayload;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

import java.time.Duration;
import java.util.UUID;

/**
 * Translates the HTTP failure surface of the DICT into the domain exception taxonomy.
 *
 * <p>Mapping rules (DICT manual + observed responses):
 * <ul>
 *   <li>{@code 404 NOT_FOUND} → {@link KeyNotFoundException} or {@link ClaimNotFoundException}</li>
 *   <li>{@code 409 CONFLICT} with {@code DUPLICATE_KEY} → {@link DuplicateKeyException}</li>
 *   <li>{@code 409 CONFLICT} with {@code CLAIM_CONFLICT} → {@link ClaimConflictException}</li>
 *   <li>{@code 422 UNPROCESSABLE_ENTITY} → {@link PolicyViolationException}</li>
 *   <li>{@code 429 TOO_MANY_REQUESTS} → {@link RateLimitedException}</li>
 *   <li>5xx → {@link DictUnavailableException}</li>
 * </ul>
 */
public final class DictErrorMapper {

    private DictErrorMapper() {}

    public static DictException toDomain(
            HttpStatusCode status,
            HttpHeaders headers,
            ProblemPayload problem,
            PixKey associatedKey,
            UUID associatedClaimId) {

        if (status.is5xxServerError()) {
            return new DictUnavailableException("DICT %s: %s".formatted(status, safeMessage(problem)));
        }
        if (status.value() == 429) {
            Duration retryAfter = parseRetryAfter(headers);
            return new RateLimitedException(retryAfter);
        }
        if (status.value() == 404) {
            if (associatedClaimId != null) {
                return new ClaimNotFoundException(associatedClaimId);
            }
            if (associatedKey != null) {
                return new KeyNotFoundException(associatedKey);
            }
            return new PolicyViolationException("NOT_FOUND", safeMessage(problem));
        }
        if (status.value() == 409 && problem != null) {
            return switch (safeCode(problem)) {
                case "DUPLICATE_KEY" -> associatedKey != null
                        ? new DuplicateKeyException(associatedKey)
                        : new PolicyViolationException("DUPLICATE_KEY", safeMessage(problem));
                case "CLAIM_CONFLICT" -> associatedKey != null
                        ? new ClaimConflictException(associatedKey)
                        : new PolicyViolationException("CLAIM_CONFLICT", safeMessage(problem));
                default -> new PolicyViolationException(safeCode(problem), safeMessage(problem));
            };
        }
        if (status.value() == 422 || status.value() == 400) {
            return new PolicyViolationException(safeCode(problem), safeMessage(problem));
        }
        return new PolicyViolationException(
                problem == null ? status.toString() : safeCode(problem),
                safeMessage(problem));
    }

    private static String safeCode(ProblemPayload problem) {
        return problem == null || problem.code() == null ? "UNKNOWN" : problem.code();
    }

    private static String safeMessage(ProblemPayload problem) {
        return problem == null || problem.message() == null ? "(no detail)" : problem.message();
    }

    private static Duration parseRetryAfter(HttpHeaders headers) {
        String header = headers.getFirst(HttpHeaders.RETRY_AFTER);
        if (header == null || header.isBlank()) {
            return Duration.ofSeconds(1);
        }
        try {
            return Duration.ofSeconds(Long.parseLong(header.trim()));
        } catch (NumberFormatException e) {
            return Duration.ofSeconds(1);
        }
    }
}
