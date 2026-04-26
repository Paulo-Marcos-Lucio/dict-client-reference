package dev.pmlsp.dict.infrastructure.simulator;

import dev.pmlsp.dict.infrastructure.http.dto.HttpDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * In-process simulator of the BCB DICT — implements the contract that {@link
 * dev.pmlsp.dict.infrastructure.http.DictHttpGateway} talks to. Active only when the
 * {@code simulator} profile is enabled.
 *
 * <p>Mounted under {@code /dict/v1} so the gateway can be pointed at
 * {@code http://localhost:8080/dict/v1} via {@code dict.endpoint.base-url}.
 */
@Slf4j
@Profile("simulator")
@RestController
@RequestMapping("/dict/v1")
@RequiredArgsConstructor
public class DictSimulatorController {

    private final InMemoryDictStore store;
    private final SimulatorBehavior behavior;

    @GetMapping("/entries/{type}/{value}")
    public ResponseEntity<?> lookup(
            @PathVariable String type,
            @PathVariable String value,
            @RequestParam(value = "payerIspb", required = false) String payerIspb) {
        if (behavior.shouldFail()) return fail();
        behavior.applyLatencyJitter();
        return store.getEntry(type, value)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HttpDtos.ProblemPayload("KEY_NOT_FOUND", "no DICT entry for given key")));
    }

    @PostMapping("/entries")
    public ResponseEntity<?> create(@RequestBody HttpDtos.CreateEntryRequest req) {
        if (behavior.shouldFail()) return fail();
        behavior.applyLatencyJitter();
        Instant now = Instant.now();
        HttpDtos.EntryPayload entry = new HttpDtos.EntryPayload(
                req.key(), req.account(), req.owner(), now, now, null);
        boolean inserted = store.putEntryIfAbsent(entry);
        if (!inserted) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new HttpDtos.ProblemPayload("DUPLICATE_KEY", "key already registered"));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(entry);
    }

    @DeleteMapping("/entries/{type}/{value}")
    public ResponseEntity<?> delete(
            @PathVariable String type,
            @PathVariable String value,
            @RequestBody(required = false) HttpDtos.DeleteEntryRequest req) {
        if (behavior.shouldFail()) return fail();
        behavior.applyLatencyJitter();
        boolean removed = store.removeEntry(type, value);
        if (!removed) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new HttpDtos.ProblemPayload("KEY_NOT_FOUND", "no DICT entry to delete"));
        }
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/claims")
    public ResponseEntity<?> startClaim(@RequestBody HttpDtos.StartClaimRequest req) {
        if (behavior.shouldFail()) return fail();
        behavior.applyLatencyJitter();
        return store.getEntry(req.key().type(), req.key().value())
                .<ResponseEntity<?>>map(donor -> {
                    Instant now = Instant.now();
                    long deadlineDays = "PORTABILITY".equals(req.type()) ? 7 : 30;
                    HttpDtos.ClaimPayload claim = new HttpDtos.ClaimPayload(
                            UUID.randomUUID(),
                            req.type(),
                            "OPEN",
                            req.key(),
                            req.claimerAccount(),
                            req.claimerOwner(),
                            req.claimerAccount().ispb(),
                            donor.account().ispb(),
                            now,
                            now.plusSeconds(deadlineDays * 24L * 3600L),
                            null,
                            null);
                    store.putClaim(claim);
                    return ResponseEntity.status(HttpStatus.CREATED).body(claim);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HttpDtos.ProblemPayload("KEY_NOT_FOUND", "no key to claim against")));
    }

    @PostMapping("/claims/{id}/acknowledge")
    public ResponseEntity<?> acknowledge(@PathVariable UUID id) {
        return transition(id, "WAITING_RESOLUTION", null);
    }

    @PostMapping("/claims/{id}/complete")
    public ResponseEntity<?> complete(@PathVariable UUID id) {
        return transition(id, "COMPLETED", Instant.now());
    }

    @PostMapping("/claims/{id}/cancel")
    public ResponseEntity<?> cancel(@PathVariable UUID id, @RequestBody HttpDtos.CancelClaimRequest req) {
        if (behavior.shouldFail()) return fail();
        behavior.applyLatencyJitter();
        return store.getClaim(id)
                .<ResponseEntity<?>>map(c -> {
                    HttpDtos.ClaimPayload updated = new HttpDtos.ClaimPayload(
                            c.claimId(), c.type(), "CANCELLED", c.key(),
                            c.claimerAccount(), c.claimerOwner(),
                            c.claimerIspb(), c.donorIspb(),
                            c.requestedAt(), c.resolutionDeadline(),
                            c.completedAt(), req.reason());
                    store.putClaim(updated);
                    return ResponseEntity.ok(updated);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HttpDtos.ProblemPayload("CLAIM_NOT_FOUND", "no claim with given id")));
    }

    @GetMapping("/claims")
    public ResponseEntity<?> list(
            @RequestParam("role") String role,
            @RequestParam("ispb") String ispb) {
        if (behavior.shouldFail()) return fail();
        behavior.applyLatencyJitter();
        return ResponseEntity.ok(List.copyOf(store.listClaims(role, ispb)));
    }

    private ResponseEntity<?> transition(UUID id, String newStatus, Instant completedAt) {
        if (behavior.shouldFail()) return fail();
        behavior.applyLatencyJitter();
        return store.getClaim(id)
                .<ResponseEntity<?>>map(c -> {
                    HttpDtos.ClaimPayload updated = new HttpDtos.ClaimPayload(
                            c.claimId(), c.type(), newStatus, c.key(),
                            c.claimerAccount(), c.claimerOwner(),
                            c.claimerIspb(), c.donorIspb(),
                            c.requestedAt(), c.resolutionDeadline(),
                            completedAt == null ? c.completedAt() : completedAt,
                            c.cancellationReason());
                    store.putClaim(updated);
                    return ResponseEntity.ok(updated);
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new HttpDtos.ProblemPayload("CLAIM_NOT_FOUND", "no claim with given id")));
    }

    private ResponseEntity<?> fail() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new HttpDtos.ProblemPayload("SIMULATED_FAILURE", "simulator injected error"));
    }
}
