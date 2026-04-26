package dev.pmlsp.dict.application.claim;

import dev.pmlsp.dict.domain.exception.DictException;
import dev.pmlsp.dict.domain.model.Claim;
import dev.pmlsp.dict.domain.model.Ispb;
import dev.pmlsp.dict.domain.model.Reason;
import dev.pmlsp.dict.domain.port.in.CancelClaimUseCase;
import dev.pmlsp.dict.domain.port.out.AuditEvent;
import dev.pmlsp.dict.domain.port.out.AuditLog;
import dev.pmlsp.dict.domain.port.out.DictEntryCache;
import dev.pmlsp.dict.domain.port.out.DictGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CancelClaimService implements CancelClaimUseCase {

    private static final String OP = "cancelClaim";

    private final DictGateway gateway;
    private final DictEntryCache cache;
    private final AuditLog audit;

    @Override
    public Claim cancel(UUID claimId, Ispb requesterIspb, Reason reason) {
        long start = System.nanoTime();
        try {
            Claim claim = gateway.cancelClaim(claimId, requesterIspb, reason);
            cache.invalidate(claim.key());
            audit.record(AuditEvent.success(OP, requesterIspb, claim.key(), elapsed(start)));
            return claim;
        } catch (DictException ex) {
            audit.record(AuditEvent.error(OP, requesterIspb, null,
                    ex.getClass().getSimpleName(), elapsed(start)));
            throw ex;
        }
    }

    private static Duration elapsed(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }
}
