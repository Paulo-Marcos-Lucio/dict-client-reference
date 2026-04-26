package dev.pmlsp.dict.application.claim;

import dev.pmlsp.dict.domain.exception.DictException;
import dev.pmlsp.dict.domain.model.Claim;
import dev.pmlsp.dict.domain.port.in.ListClaimsUseCase;
import dev.pmlsp.dict.domain.port.out.AuditEvent;
import dev.pmlsp.dict.domain.port.out.AuditLog;
import dev.pmlsp.dict.domain.port.out.DictGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ListClaimsService implements ListClaimsUseCase {

    private static final String OP = "listClaims";

    private final DictGateway gateway;
    private final AuditLog audit;

    @Override
    public List<Claim> list(ListClaimsQuery query) {
        long start = System.nanoTime();
        try {
            List<Claim> claims = gateway.listClaims(query.role(), query.ispb());
            audit.record(AuditEvent.success(OP, query.ispb(), null, elapsed(start)));
            return claims;
        } catch (DictException ex) {
            audit.record(AuditEvent.error(OP, query.ispb(), null,
                    ex.getClass().getSimpleName(), elapsed(start)));
            throw ex;
        }
    }

    private static Duration elapsed(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }
}
