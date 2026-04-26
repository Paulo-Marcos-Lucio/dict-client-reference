package dev.pmlsp.dict.infrastructure.http.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Wire-format DTOs exchanged with the DICT (real or simulator). Kept deliberately
 * decoupled from the domain so a future XML or BCB-specific naming convention can be
 * introduced without rippling into use cases.
 */
public final class HttpDtos {

    private HttpDtos() {}

    public record KeyPayload(String type, String value) {}

    public record AccountPayload(String ispb, String branch, String number, String type) {}

    public record OwnerPayload(String type, String document, String name, String tradeName) {}

    public record EntryPayload(
            KeyPayload key,
            AccountPayload account,
            OwnerPayload owner,
            Instant createdAt,
            Instant keyOwnershipDate,
            String openClaimType) {}

    public record CreateEntryRequest(
            KeyPayload key,
            AccountPayload account,
            OwnerPayload owner) {}

    public record StartClaimRequest(
            String type,
            KeyPayload key,
            AccountPayload claimerAccount,
            OwnerPayload claimerOwner) {}

    public record CancelClaimRequest(String reason) {}

    public record DeleteEntryRequest(String reason) {}

    public record ClaimPayload(
            UUID claimId,
            String type,
            String status,
            KeyPayload key,
            AccountPayload claimerAccount,
            OwnerPayload claimerOwner,
            String claimerIspb,
            String donorIspb,
            Instant requestedAt,
            Instant resolutionDeadline,
            Instant completedAt,
            String cancellationReason) {}

    public record ProblemPayload(String code, String message) {}
}
