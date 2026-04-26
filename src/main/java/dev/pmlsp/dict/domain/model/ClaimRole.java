package dev.pmlsp.dict.domain.model;

/**
 * Perspective from which a participant queries open claims.
 *
 * <ul>
 *   <li>{@link #DONOR} — claims that target keys held by this participant</li>
 *   <li>{@link #CLAIMER} — claims this participant has opened against other participants</li>
 * </ul>
 */
public enum ClaimRole {
    DONOR,
    CLAIMER
}
