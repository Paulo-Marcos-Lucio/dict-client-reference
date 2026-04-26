package dev.pmlsp.dict.application.claim;

import dev.pmlsp.dict.domain.exception.DictException;
import dev.pmlsp.dict.domain.model.Claim;
import dev.pmlsp.dict.domain.port.in.StartClaimUseCase;
import dev.pmlsp.dict.domain.port.out.AuditEvent;
import dev.pmlsp.dict.domain.port.out.AuditLog;
import dev.pmlsp.dict.domain.port.out.DictEntryCache;
import dev.pmlsp.dict.domain.port.out.DictGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class StartClaimService implements StartClaimUseCase {

    private static final String OP = "startClaim";

    private final DictGateway gateway;
    private final DictEntryCache cache;
    private final AuditLog audit;

    @Override
    public Claim start(StartClaimCommand cmd) {
        long start = System.nanoTime();
        try {
            Claim claim = gateway.startClaim(
                    cmd.type(), cmd.key(), cmd.claimerAccount(), cmd.claimerOwner(), cmd.claimerIspb());
            cache.invalidate(cmd.key());
            audit.record(AuditEvent.success(OP, cmd.claimerIspb(), cmd.key(), elapsed(start)));
            return claim;
        } catch (DictException ex) {
            audit.record(AuditEvent.error(OP, cmd.claimerIspb(), cmd.key(),
                    ex.getClass().getSimpleName(), elapsed(start)));
            throw ex;
        }
    }

    private static Duration elapsed(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }
}
