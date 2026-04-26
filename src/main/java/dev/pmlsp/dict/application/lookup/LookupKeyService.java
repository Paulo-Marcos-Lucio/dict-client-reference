package dev.pmlsp.dict.application.lookup;

import dev.pmlsp.dict.domain.exception.DictException;
import dev.pmlsp.dict.domain.exception.KeyNotFoundException;
import dev.pmlsp.dict.domain.model.DictEntry;
import dev.pmlsp.dict.domain.model.Ispb;
import dev.pmlsp.dict.domain.model.PixKey;
import dev.pmlsp.dict.domain.policy.CacheTtlPolicy;
import dev.pmlsp.dict.domain.port.in.LookupKeyUseCase;
import dev.pmlsp.dict.domain.port.out.AuditEvent;
import dev.pmlsp.dict.domain.port.out.AuditLog;
import dev.pmlsp.dict.domain.port.out.DictEntryCache;
import dev.pmlsp.dict.domain.port.out.DictGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LookupKeyService implements LookupKeyUseCase {

    private static final String OP = "lookup";

    private final DictGateway gateway;
    private final DictEntryCache cache;
    private final CacheTtlPolicy ttlPolicy;
    private final AuditLog audit;

    @Override
    public DictEntry lookup(LookupQuery query) {
        PixKey key = query.key();
        Ispb requester = query.payerIspb();
        long start = System.nanoTime();

        Optional<DictEntry> cached = cache.get(key);
        if (cached.isPresent()) {
            Duration dur = elapsed(start);
            audit.record(AuditEvent.cacheHit(OP, requester, key, dur));
            return cached.get();
        }

        try {
            Optional<DictEntry> fetched = gateway.lookup(key, requester);
            Duration dur = elapsed(start);
            if (fetched.isEmpty()) {
                audit.record(AuditEvent.notFound(OP, requester, key, dur));
                throw new KeyNotFoundException(key);
            }
            DictEntry entry = fetched.get();
            Duration ttl = ttlPolicy.ttlFor(entry);
            if (!ttl.isZero() && !ttl.isNegative()) {
                cache.put(key, entry, ttl);
            }
            audit.record(AuditEvent.success(OP, requester, key, dur));
            return entry;
        } catch (DictException ex) {
            audit.record(AuditEvent.error(OP, requester, key, ex.getClass().getSimpleName(), elapsed(start)));
            throw ex;
        }
    }

    private static Duration elapsed(long startNanos) {
        return Duration.ofNanos(System.nanoTime() - startNanos);
    }
}
