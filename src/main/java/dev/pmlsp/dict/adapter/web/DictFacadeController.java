package dev.pmlsp.dict.adapter.web;

import dev.pmlsp.dict.adapter.web.dto.WebDtos;
import dev.pmlsp.dict.domain.model.Account;
import dev.pmlsp.dict.domain.model.AccountType;
import dev.pmlsp.dict.domain.model.Claim;
import dev.pmlsp.dict.domain.model.ClaimRole;
import dev.pmlsp.dict.domain.model.ClaimType;
import dev.pmlsp.dict.domain.model.DictEntry;
import dev.pmlsp.dict.domain.model.Ispb;
import dev.pmlsp.dict.domain.model.Owner;
import dev.pmlsp.dict.domain.model.OwnerType;
import dev.pmlsp.dict.domain.model.PixKey;
import dev.pmlsp.dict.domain.model.PixKeyType;
import dev.pmlsp.dict.domain.model.Reason;
import dev.pmlsp.dict.domain.port.in.AcknowledgeClaimUseCase;
import dev.pmlsp.dict.domain.port.in.CancelClaimUseCase;
import dev.pmlsp.dict.domain.port.in.CompleteClaimUseCase;
import dev.pmlsp.dict.domain.port.in.CreateEntryUseCase;
import dev.pmlsp.dict.domain.port.in.DeleteEntryUseCase;
import dev.pmlsp.dict.domain.port.in.ListClaimsUseCase;
import dev.pmlsp.dict.domain.port.in.LookupKeyUseCase;
import dev.pmlsp.dict.domain.port.in.StartClaimUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Demo HTTP facade over the use cases — handy for {@code requests.http} sessions and IT
 * tests, not the only way to consume this library. Real adopters typically inject the
 * use case interfaces directly.
 */
@RestController
@RequestMapping("/v1/dict")
@RequiredArgsConstructor
public class DictFacadeController {

    private final LookupKeyUseCase lookup;
    private final CreateEntryUseCase create;
    private final DeleteEntryUseCase delete;
    private final StartClaimUseCase startClaim;
    private final AcknowledgeClaimUseCase acknowledge;
    private final CompleteClaimUseCase complete;
    private final CancelClaimUseCase cancel;
    private final ListClaimsUseCase listClaims;

    @GetMapping("/entries/{type}/{value}")
    public ResponseEntity<WebDtos.EntryResponse> lookupKey(
            @PathVariable PixKeyType type,
            @PathVariable String value,
            @RequestHeader("X-Payer-Ispb") String payerIspb) {
        DictEntry entry = lookup.lookup(new LookupKeyUseCase.LookupQuery(
                new PixKey(type, value), Ispb.of(payerIspb)));
        return ResponseEntity.ok(toResponse(entry));
    }

    @PostMapping("/entries")
    public ResponseEntity<WebDtos.EntryResponse> createEntry(
            @Valid @RequestBody WebDtos.EntryRequest body,
            @RequestHeader("X-Participant-Ispb") String requesterIspb) {
        DictEntry created = create.create(new CreateEntryUseCase.CreateEntryCommand(
                new PixKey(PixKeyType.valueOf(body.key().type()), body.key().value()),
                toAccount(body.account()),
                toOwner(body.owner()),
                Ispb.of(requesterIspb)));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(created));
    }

    @DeleteMapping("/entries/{type}/{value}")
    public ResponseEntity<Void> deleteEntry(
            @PathVariable PixKeyType type,
            @PathVariable String value,
            @Valid @RequestBody WebDtos.DeleteRequest body,
            @RequestHeader("X-Participant-Ispb") String requesterIspb) {
        delete.delete(new DeleteEntryUseCase.DeleteEntryCommand(
                new PixKey(type, value), Reason.valueOf(body.reason()), Ispb.of(requesterIspb)));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/claims")
    public ResponseEntity<WebDtos.ClaimResponse> startClaim(
            @Valid @RequestBody WebDtos.StartClaimRequest body,
            @RequestHeader("X-Participant-Ispb") String claimerIspb) {
        Claim claim = startClaim.start(new StartClaimUseCase.StartClaimCommand(
                ClaimType.valueOf(body.type()),
                new PixKey(PixKeyType.valueOf(body.key().type()), body.key().value()),
                toAccount(body.claimerAccount()),
                toOwner(body.claimerOwner()),
                Ispb.of(claimerIspb)));
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(claim));
    }

    @PostMapping("/claims/{id}/acknowledge")
    public ResponseEntity<WebDtos.ClaimResponse> acknowledgeClaim(
            @PathVariable UUID id,
            @RequestHeader("X-Participant-Ispb") String donorIspb) {
        return ResponseEntity.ok(toResponse(acknowledge.acknowledge(id, Ispb.of(donorIspb))));
    }

    @PostMapping("/claims/{id}/complete")
    public ResponseEntity<WebDtos.ClaimResponse> completeClaim(
            @PathVariable UUID id,
            @RequestHeader("X-Participant-Ispb") String requesterIspb) {
        return ResponseEntity.ok(toResponse(complete.complete(id, Ispb.of(requesterIspb))));
    }

    @PostMapping("/claims/{id}/cancel")
    public ResponseEntity<WebDtos.ClaimResponse> cancelClaim(
            @PathVariable UUID id,
            @Valid @RequestBody WebDtos.CancelClaimRequest body,
            @RequestHeader("X-Participant-Ispb") String requesterIspb) {
        return ResponseEntity.ok(toResponse(
                cancel.cancel(id, Ispb.of(requesterIspb), Reason.valueOf(body.reason()))));
    }

    @GetMapping("/claims")
    public ResponseEntity<List<WebDtos.ClaimResponse>> listClaims(
            @RequestParam("role") ClaimRole role,
            @RequestParam("ispb") String ispb) {
        List<Claim> claims = listClaims.list(new ListClaimsUseCase.ListClaimsQuery(role, Ispb.of(ispb)));
        return ResponseEntity.ok(claims.stream().map(DictFacadeController::toResponse).toList());
    }

    // ---------- mappers ----------

    private static Account toAccount(WebDtos.AccountDto dto) {
        return new Account(Ispb.of(dto.ispb()), dto.branch(), dto.number(), AccountType.valueOf(dto.type()));
    }

    private static Owner toOwner(WebDtos.OwnerDto dto) {
        return new Owner(OwnerType.valueOf(dto.type()), dto.document(), dto.name(), dto.tradeName());
    }

    private static WebDtos.KeyDto toDto(PixKey k) {
        return new WebDtos.KeyDto(k.type().name(), k.value());
    }

    private static WebDtos.AccountDto toDto(Account a) {
        return new WebDtos.AccountDto(a.ispb().value(), a.branch(), a.number(), a.type().name());
    }

    private static WebDtos.OwnerDto toDto(Owner o) {
        return new WebDtos.OwnerDto(o.type().name(), o.document(), o.name(), o.tradeName());
    }

    private static WebDtos.EntryResponse toResponse(DictEntry e) {
        return new WebDtos.EntryResponse(
                toDto(e.key()), toDto(e.account()), toDto(e.owner()),
                e.createdAt(), e.keyOwnershipDate(),
                e.openClaim().map(Enum::name).orElse(null));
    }

    private static WebDtos.ClaimResponse toResponse(Claim c) {
        return new WebDtos.ClaimResponse(
                c.claimId(), c.type().name(), c.status().name(),
                toDto(c.key()), toDto(c.claimerAccount()), toDto(c.claimerOwner()),
                c.claimerIspb().value(), c.donorIspb().value(),
                c.requestedAt(), c.resolutionDeadline(),
                c.completedAtOpt().orElse(null),
                c.cancellationReasonOpt().map(Enum::name).orElse(null));
    }
}
