package dev.pmlsp.dict.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Aggregate root for a DICT claim (portability or ownership).
 *
 * <p>State machine:
 * <pre>
 * OPEN ──► WAITING_RESOLUTION ──► CONFIRMED ──► COMPLETED
 *   │           │                     │
 *   └───────────┴─────────────────────┴─► CANCELLED
 * </pre>
 *
 * <p>Transitions are enforced by {@link #acknowledge()}, {@link #confirm()},
 * {@link #complete()} and {@link #cancel(Reason)}. Each returns a new immutable
 * snapshot — instances are never mutated in place.
 */
public record Claim(
        UUID claimId,
        ClaimType type,
        ClaimStatus status,
        PixKey key,
        Account claimerAccount,
        Owner claimerOwner,
        Ispb claimerIspb,
        Ispb donorIspb,
        Instant requestedAt,
        Instant resolutionDeadline,
        Instant completedAt,
        Reason cancellationReason) {

    public Claim {
        Objects.requireNonNull(claimId, "claimId");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(claimerAccount, "claimerAccount");
        Objects.requireNonNull(claimerOwner, "claimerOwner");
        Objects.requireNonNull(claimerIspb, "claimerIspb");
        Objects.requireNonNull(donorIspb, "donorIspb");
        Objects.requireNonNull(requestedAt, "requestedAt");
        Objects.requireNonNull(resolutionDeadline, "resolutionDeadline");
        if (claimerIspb.equals(donorIspb)) {
            throw new IllegalArgumentException("claimer and donor ISPB must differ");
        }
    }

    public static Claim open(
            ClaimType type,
            PixKey key,
            Account claimerAccount,
            Owner claimerOwner,
            Ispb claimerIspb,
            Ispb donorIspb) {
        Instant now = Instant.now();
        return new Claim(
                UUID.randomUUID(),
                type,
                ClaimStatus.OPEN,
                key,
                claimerAccount,
                claimerOwner,
                claimerIspb,
                donorIspb,
                now,
                now.plus(type.resolutionWindow()),
                null,
                null);
    }

    public Claim acknowledge() {
        requireStatus(ClaimStatus.OPEN, "acknowledge");
        return withStatus(ClaimStatus.WAITING_RESOLUTION);
    }

    public Claim confirm() {
        requireStatus(ClaimStatus.WAITING_RESOLUTION, "confirm");
        return withStatus(ClaimStatus.CONFIRMED);
    }

    public Claim complete() {
        requireStatus(ClaimStatus.CONFIRMED, "complete");
        return new Claim(
                claimId, type, ClaimStatus.COMPLETED, key,
                claimerAccount, claimerOwner, claimerIspb, donorIspb,
                requestedAt, resolutionDeadline, Instant.now(), null);
    }

    public Claim cancel(Reason reason) {
        Objects.requireNonNull(reason, "reason");
        if (status.isTerminal()) {
            throw new IllegalStateException("cannot cancel claim in terminal status: " + status);
        }
        return new Claim(
                claimId, type, ClaimStatus.CANCELLED, key,
                claimerAccount, claimerOwner, claimerIspb, donorIspb,
                requestedAt, resolutionDeadline, completedAt, reason);
    }

    public Optional<Instant> completedAtOpt() {
        return Optional.ofNullable(completedAt);
    }

    public Optional<Reason> cancellationReasonOpt() {
        return Optional.ofNullable(cancellationReason);
    }

    private void requireStatus(ClaimStatus expected, String op) {
        if (status != expected) {
            throw new IllegalStateException(
                    "cannot %s claim in status %s (expected %s)".formatted(op, status, expected));
        }
    }

    private Claim withStatus(ClaimStatus next) {
        return new Claim(claimId, type, next, key,
                claimerAccount, claimerOwner, claimerIspb, donorIspb,
                requestedAt, resolutionDeadline, completedAt, cancellationReason);
    }
}
