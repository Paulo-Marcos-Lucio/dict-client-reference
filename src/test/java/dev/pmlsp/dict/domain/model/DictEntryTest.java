package dev.pmlsp.dict.domain.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class DictEntryTest {

    private static final PixKey KEY = new PixKey(PixKeyType.EMAIL, "a@b.co");
    private static final Account ACCOUNT = new Account(
            Ispb.of("12345678"), "0001", "0000111111", AccountType.CACC);
    private static final Owner OWNER = Owner.naturalPerson("12345678901", "Alice");

    @Test
    void factoryProducesEntryWithoutOpenClaim() {
        DictEntry entry = DictEntry.of(KEY, ACCOUNT, OWNER);

        assertThat(entry.hasOpenClaim()).isFalse();
        assertThat(entry.openClaim()).isEmpty();
        assertThat(entry.createdAt()).isNotNull();
        assertThat(entry.keyOwnershipDate()).isNotNull();
    }

    @Test
    void recognizesEntryWithOpenClaim() {
        Instant now = Instant.now();
        DictEntry contested = new DictEntry(KEY, ACCOUNT, OWNER, now, now, ClaimType.OWNERSHIP);

        assertThat(contested.hasOpenClaim()).isTrue();
        assertThat(contested.openClaim()).contains(ClaimType.OWNERSHIP);
    }
}
