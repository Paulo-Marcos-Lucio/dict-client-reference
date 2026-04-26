package dev.pmlsp.dict.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Aggregate root representing a Pix key registration in the DICT.
 *
 * <p>An entry is the binding {@code key → account → owner}, plus metadata that influences
 * downstream caching and operational decisions:
 * <ul>
 *   <li>{@link #createdAt} — when the entry was first created in the DICT</li>
 *   <li>{@link #keyOwnershipDate} — when the current owner's ownership was last recognized</li>
 *   <li>{@link #openClaimType} — present when there's a pending claim (caller MUST NOT cache)</li>
 * </ul>
 */
public record DictEntry(
        PixKey key,
        Account account,
        Owner owner,
        Instant createdAt,
        Instant keyOwnershipDate,
        ClaimType openClaimType) {

    public DictEntry {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(account, "account");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(keyOwnershipDate, "keyOwnershipDate");
        // openClaimType is nullable — represents Optional intentionally
    }

    public static DictEntry of(PixKey key, Account account, Owner owner) {
        Instant now = Instant.now();
        return new DictEntry(key, account, owner, now, now, null);
    }

    public Optional<ClaimType> openClaim() {
        return Optional.ofNullable(openClaimType);
    }

    public boolean hasOpenClaim() {
        return openClaimType != null;
    }
}
