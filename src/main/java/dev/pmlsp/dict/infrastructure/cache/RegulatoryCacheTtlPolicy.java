package dev.pmlsp.dict.infrastructure.cache;

import dev.pmlsp.dict.domain.model.DictEntry;
import dev.pmlsp.dict.domain.model.PixKeyType;
import dev.pmlsp.dict.domain.policy.CacheTtlPolicy;
import dev.pmlsp.dict.infrastructure.config.DictClientProperties;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;

/**
 * Default {@link CacheTtlPolicy} that:
 * <ol>
 *   <li>Honors the per-key-type TTL configured in {@code dict.cache.ttl.*}</li>
 *   <li>Clamps it to the regulatory maximum exposed via the constants on the interface</li>
 *   <li>Returns {@link CacheTtlPolicy#ZERO} when the entry has any open claim</li>
 * </ol>
 */
@Slf4j
public class RegulatoryCacheTtlPolicy implements CacheTtlPolicy {

    private final DictClientProperties.Cache.Ttl configured;

    public RegulatoryCacheTtlPolicy(DictClientProperties.Cache.Ttl configured) {
        this.configured = configured;
    }

    @Override
    public Duration ttlFor(DictEntry entry) {
        if (entry.hasOpenClaim()) {
            return ZERO;
        }
        Duration max = maxFor(entry.key().type());
        Duration desired = configured.forKey(entry.key().type());
        if (desired.compareTo(max) > 0) {
            log.warn("dict.cache.ttl.clamped keyType={} configured={}s max={}s",
                    entry.key().type(), desired.toSeconds(), max.toSeconds());
            return max;
        }
        return desired;
    }

    private static Duration maxFor(PixKeyType type) {
        return switch (type) {
            case CPF -> MAX_CPF;
            case CNPJ -> MAX_CNPJ;
            case EMAIL -> MAX_EMAIL;
            case PHONE -> MAX_PHONE;
            case EVP -> MAX_EVP;
        };
    }
}
