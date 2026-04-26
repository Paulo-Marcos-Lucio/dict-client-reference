package dev.pmlsp.dict.application.entry;

import dev.pmlsp.dict.domain.exception.DictException;
import dev.pmlsp.dict.domain.port.in.DeleteEntryUseCase;
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
public class DeleteEntryService implements DeleteEntryUseCase {

    private static final String OP = "deleteEntry";

    private final DictGateway gateway;
    private final DictEntryCache cache;
    private final AuditLog audit;

    @Override
    public void delete(DeleteEntryCommand cmd) {
        long start = System.nanoTime();
        try {
            gateway.deleteEntry(cmd.key(), cmd.reason(), cmd.requesterIspb());
            cache.invalidate(cmd.key());
            audit.record(AuditEvent.success(OP, cmd.requesterIspb(), cmd.key(), elapsed(start)));
        } catch (DictException ex) {
            audit.record(AuditEvent.error(OP, cmd.requesterIspb(), cmd.key(),
                    ex.getClass().getSimpleName(), elapsed(start)));
            throw ex;
        }
    }

    private static Duration elapsed(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }
}
