package dev.pmlsp.dict.adapter.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.UUID;

public final class WebDtos {

    private WebDtos() {}

    public record KeyDto(
            @NotNull String type,
            @NotBlank String value) {}

    public record AccountDto(
            @NotBlank @Pattern(regexp = "\\d{8}") String ispb,
            @NotBlank String branch,
            @NotBlank String number,
            @NotNull String type) {}

    public record OwnerDto(
            @NotNull String type,
            @NotBlank String document,
            @NotBlank String name,
            String tradeName) {}

    public record EntryRequest(
            @Valid @NotNull KeyDto key,
            @Valid @NotNull AccountDto account,
            @Valid @NotNull OwnerDto owner) {}

    public record EntryResponse(
            KeyDto key,
            AccountDto account,
            OwnerDto owner,
            Instant createdAt,
            Instant keyOwnershipDate,
            String openClaimType) {}

    public record DeleteRequest(@NotNull String reason) {}

    public record StartClaimRequest(
            @NotNull String type,
            @Valid @NotNull KeyDto key,
            @Valid @NotNull AccountDto claimerAccount,
            @Valid @NotNull OwnerDto claimerOwner) {}

    public record CancelClaimRequest(@NotNull String reason) {}

    public record ClaimResponse(
            UUID claimId,
            String type,
            String status,
            KeyDto key,
            AccountDto claimerAccount,
            OwnerDto claimerOwner,
            String claimerIspb,
            String donorIspb,
            Instant requestedAt,
            Instant resolutionDeadline,
            Instant completedAt,
            String cancellationReason) {}

    public record ProblemDetail(
            String type,
            String title,
            int status,
            String detail,
            String code) {}
}
