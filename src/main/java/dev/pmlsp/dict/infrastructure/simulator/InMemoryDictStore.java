package dev.pmlsp.dict.infrastructure.simulator;

import dev.pmlsp.dict.infrastructure.http.dto.HttpDtos;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store backing the {@link DictSimulatorController}. Pre-populated with one entry
 * per key type so the simulator works out-of-the-box with the {@code requests.http} samples.
 */
public class InMemoryDictStore {

    private final Map<KeyId, HttpDtos.EntryPayload> entries = new ConcurrentHashMap<>();
    private final Map<UUID, HttpDtos.ClaimPayload> claims = new ConcurrentHashMap<>();

    public InMemoryDictStore() {
        seed();
    }

    public Optional<HttpDtos.EntryPayload> getEntry(String type, String value) {
        return Optional.ofNullable(entries.get(new KeyId(type, value)));
    }

    public boolean putEntryIfAbsent(HttpDtos.EntryPayload entry) {
        KeyId id = new KeyId(entry.key().type(), entry.key().value());
        return entries.putIfAbsent(id, entry) == null;
    }

    public boolean removeEntry(String type, String value) {
        return entries.remove(new KeyId(type, value)) != null;
    }

    public void putClaim(HttpDtos.ClaimPayload claim) {
        claims.put(claim.claimId(), claim);
    }

    public Optional<HttpDtos.ClaimPayload> getClaim(UUID claimId) {
        return Optional.ofNullable(claims.get(claimId));
    }

    public Collection<HttpDtos.ClaimPayload> listClaims(String role, String ispb) {
        return claims.values().stream()
                .filter(c -> "donor".equalsIgnoreCase(role)
                        ? c.donorIspb().equals(ispb)
                        : c.claimerIspb().equals(ispb))
                .toList();
    }

    private void seed() {
        Instant now = Instant.now();

        HttpDtos.AccountPayload acc1 = new HttpDtos.AccountPayload("12345678", "0001", "0000111111", "CACC");
        HttpDtos.OwnerPayload alice = new HttpDtos.OwnerPayload("NATURAL_PERSON", "12345678901", "Alice Silva", null);

        List<HttpDtos.EntryPayload> seeds = new ArrayList<>();
        seeds.add(new HttpDtos.EntryPayload(
                new HttpDtos.KeyPayload("CPF", "12345678901"), acc1, alice, now, now, null));
        seeds.add(new HttpDtos.EntryPayload(
                new HttpDtos.KeyPayload("EMAIL", "loja@merchant.com"), acc1,
                new HttpDtos.OwnerPayload("LEGAL_PERSON", "12345678000199", "Loja Merchant LTDA", "Merchant"),
                now, now, null));
        seeds.add(new HttpDtos.EntryPayload(
                new HttpDtos.KeyPayload("PHONE", "+5511987654321"), acc1, alice, now, now, null));
        seeds.add(new HttpDtos.EntryPayload(
                new HttpDtos.KeyPayload("CNPJ", "12345678000199"), acc1,
                new HttpDtos.OwnerPayload("LEGAL_PERSON", "12345678000199", "Loja Merchant LTDA", "Merchant"),
                now, now, null));
        seeds.add(new HttpDtos.EntryPayload(
                new HttpDtos.KeyPayload("EVP", "550e8400-e29b-41d4-a716-446655440000"), acc1, alice, now, now, null));

        seeds.forEach(this::putEntryIfAbsent);
    }

    private record KeyId(String type, String value) {}
}
