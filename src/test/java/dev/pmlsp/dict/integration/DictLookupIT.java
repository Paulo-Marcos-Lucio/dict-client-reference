package dev.pmlsp.dict.integration;

import dev.pmlsp.dict.domain.exception.KeyNotFoundException;
import dev.pmlsp.dict.domain.model.Ispb;
import dev.pmlsp.dict.domain.model.PixKey;
import dev.pmlsp.dict.domain.model.PixKeyType;
import dev.pmlsp.dict.domain.port.in.LookupKeyUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end: use case → HTTP gateway → real simulator → response → cache.
 * Exercises the full hexagonal stack against the in-process simulator.
 */
class DictLookupIT extends AbstractIntegrationIT {

    @Autowired
    LookupKeyUseCase lookup;

    @Test
    void resolvesSeededEmailKey() {
        var entry = lookup.lookup(new LookupKeyUseCase.LookupQuery(
                new PixKey(PixKeyType.EMAIL, "loja@merchant.com"),
                Ispb.of("12345678")));

        assertThat(entry.key().value()).isEqualTo("loja@merchant.com");
        assertThat(entry.account().ispb().value()).isEqualTo("12345678");
        assertThat(entry.owner().name()).isEqualTo("Loja Merchant LTDA");
    }

    @Test
    void secondCallHitsCache() {
        var query = new LookupKeyUseCase.LookupQuery(
                new PixKey(PixKeyType.CPF, "12345678901"),
                Ispb.of("12345678"));

        var first = lookup.lookup(query);
        var second = lookup.lookup(query);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void unknownKeyMapsToDomainException() {
        assertThatThrownBy(() -> lookup.lookup(new LookupKeyUseCase.LookupQuery(
                new PixKey(PixKeyType.EMAIL, "unknown@nowhere.com"),
                Ispb.of("12345678"))))
                .isInstanceOf(KeyNotFoundException.class);
    }
}
