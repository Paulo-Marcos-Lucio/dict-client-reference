package dev.pmlsp.dict.domain.model;

/**
 * Reason codes accepted by the BCB DICT for entry deletion or claim cancellation.
 * Mirrors the {@code reason} enumeration documented in the DICT operations manual.
 */
public enum Reason {
    USER_REQUESTED,
    ACCOUNT_CLOSURE,
    BRANCH_TRANSFER,
    RECONCILIATION,
    FRAUD,
    MASS_OPERATION,
    REJECTED_BY_PSP
}
