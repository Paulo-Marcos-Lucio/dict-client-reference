package dev.pmlsp.dict.infrastructure.cache;

import dev.pmlsp.dict.domain.model.Account;
import dev.pmlsp.dict.domain.model.AccountType;
import dev.pmlsp.dict.domain.model.ClaimType;
import dev.pmlsp.dict.domain.model.DictEntry;
import dev.pmlsp.dict.domain.model.Ispb;
import dev.pmlsp.dict.domain.model.Owner;
import dev.pmlsp.dict.domain.model.PixKey;
import dev.pmlsp.dict.domain.model.PixKeyType;
import dev.pmlsp.dict.domain.policy.CacheTtlPolicy;
import dev.pmlsp.dict.infrastructure.config.DictClientProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RegulatoryCacheTtlPolicyTest {

    private static final Account ACC = new Account(Ispb.of("12345678"), "0001", "111", AccountType.CACC);
    private static final Owner OWNER = Owner.naturalPerson("12345678901", "Alice");

    private static DictEntry entry(PixKeyType type, String value, ClaimType openClaim) {
        Instant now = Instant.now();
        return new DictEntry(new PixKey(type, value), ACC, OWNER, now, now, openClaim);
    }

    private static DictClientProperties.Cache.Ttl ttl(
            Duration cpf, Duration cnpj, Duration email, Duration phone, Duration evp) {
        return new DictClientProperties.Cache.Ttl(cpf, cnpj, email, phone, evp);
    }

    @Test
    void returnsZeroWhenEntryHasOpenClaim() {
        CacheTtlPolicy policy = new RegulatoryCacheTtlPolicy(
                ttl(Duration.ofSeconds(30), Duration.ofSeconds(60), Duration.ofSeconds(60),
                        Duration.ofSeconds(60), Duration.ofSeconds(30)));

        Duration ttl = policy.ttlFor(entry(PixKeyType.EMAIL, "a@b.co", ClaimType.OWNERSHIP));

        assertThat(ttl).isEqualTo(Duration.ZERO);
    }

    @Test
    void honorsConfiguredTtlBelowMax() {
        CacheTtlPolicy policy = new RegulatoryCacheTtlPolicy(
                ttl(Duration.ofSeconds(20), Duration.ofSeconds(60), Duration.ofSeconds(60),
                        Duration.ofSeconds(60), Duration.ofSeconds(30)));

        Duration ttl = policy.ttlFor(entry(PixKeyType.CPF, "12345678901", null));

        assertThat(ttl).isEqualTo(Duration.ofSeconds(20));
    }

    @Test
    void clampsConfiguredTtlAboveRegulatoryMax() {
        // Config asks for 600s on EVP (max is 60s)
        CacheTtlPolicy policy = new RegulatoryCacheTtlPolicy(
                ttl(Duration.ofSeconds(30), Duration.ofSeconds(60), Duration.ofSeconds(60),
                        Duration.ofSeconds(60), Duration.ofSeconds(600)));

        Duration ttl = policy.ttlFor(entry(PixKeyType.EVP,
                "550e8400-e29b-41d4-a716-446655440000", null));

        assertThat(ttl).isEqualTo(CacheTtlPolicy.MAX_EVP);
    }
}
