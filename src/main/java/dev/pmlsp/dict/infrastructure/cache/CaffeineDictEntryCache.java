package dev.pmlsp.dict.infrastructure.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import dev.pmlsp.dict.domain.model.DictEntry;
import dev.pmlsp.dict.domain.model.PixKey;
import dev.pmlsp.dict.domain.port.out.DictEntryCache;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine-backed cache that supports a different TTL per entry — required because
 * each {@link PixKey} type has its own regulatory cap and entries with open claims
 * must not be cached at all.
 */
public class CaffeineDictEntryCache implements DictEntryCache {

    private final Cache<PixKey, Entry> cache;

    public CaffeineDictEntryCache(int maxSize) {
        this.cache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfter(new Expiry<PixKey, Entry>() {
                    @Override
                    public long expireAfterCreate(@NonNull PixKey key, @NonNull Entry value, long currentTime) {
                        return TimeUnit.NANOSECONDS.convert(value.ttl().toNanos(), TimeUnit.NANOSECONDS);
                    }

                    @Override
                    public long expireAfterUpdate(@NonNull PixKey key, @NonNull Entry value, long currentTime, long currentDuration) {
                        return TimeUnit.NANOSECONDS.convert(value.ttl().toNanos(), TimeUnit.NANOSECONDS);
                    }

                    @Override
                    public long expireAfterRead(@NonNull PixKey key, @NonNull Entry value, long currentTime, long currentDuration) {
                        return currentDuration;
                    }
                })
                .build();
    }

    @Override
    public Optional<DictEntry> get(PixKey key) {
        Entry stored = cache.getIfPresent(key);
        return stored == null ? Optional.empty() : Optional.of(stored.value());
    }

    @Override
    public void put(PixKey key, DictEntry entry, Duration ttl) {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        cache.put(key, new Entry(entry, ttl));
    }

    @Override
    public void invalidate(PixKey key) {
        cache.invalidate(key);
    }

    private record Entry(DictEntry value, Duration ttl) {}
}
