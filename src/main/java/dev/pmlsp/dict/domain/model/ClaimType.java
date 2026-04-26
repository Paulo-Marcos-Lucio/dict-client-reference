package dev.pmlsp.dict.domain.model;

import java.time.Duration;

/**
 * BCB DICT claim taxonomy.
 *
 * <ul>
 *   <li>{@link #PORTABILITY} — same person/entity moves the key from a previous bank to a new one</li>
 *   <li>{@link #OWNERSHIP} — a different person/entity claims ownership over the key, contesting the current registration</li>
 * </ul>
 *
 * Resolution windows differ per type and are encoded in {@link #resolutionWindow()}.
 */
public enum ClaimType {
    PORTABILITY(Duration.ofDays(7)),
    OWNERSHIP(Duration.ofDays(30));

    private final Duration resolutionWindow;

    ClaimType(Duration resolutionWindow) {
        this.resolutionWindow = resolutionWindow;
    }

    public Duration resolutionWindow() {
        return resolutionWindow;
    }
}
