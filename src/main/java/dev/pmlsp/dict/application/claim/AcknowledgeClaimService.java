package dev.pmlsp.dict.application.claim;

import dev.pmlsp.dict.domain.exception.DictException;
import dev.pmlsp.dict.domain.model.Claim;
import dev.pmlsp.dict.domain.model.Ispb;
import dev.pmlsp.dict.domain.port.in.AcknowledgeClaimUseCase;
import dev.pmlsp.dict.domain.port.out.AuditEvent;
import dev.pmlsp.dict.domain.port.out.AuditLog;
import dev.pmlsp.dict.domain.port.out.DictGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AcknowledgeClaimService implements AcknowledgeClaimUseCase {

    private static final String OP = "acknowledgeClaim";

    private final DictGateway gateway;
    private final AuditLog audit;

    @Override
    public Claim acknowledge(UUID claimId, Ispb donorIspb) {
        long start = System.nanoTime();
        try {
            Claim claim = gateway.acknowledgeClaim(claimId, donorIspb);
            audit.record(AuditEvent.success(OP, donorIspb, claim.key(), elapsed(start)));
            return claim;
        } catch (DictException ex) {
            audit.record(AuditEvent.error(OP, donorIspb, null,
                    ex.getClass().getSimpleName(), elapsed(start)));
            throw ex;
        }
    }

    private static Duration elapsed(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }
}
