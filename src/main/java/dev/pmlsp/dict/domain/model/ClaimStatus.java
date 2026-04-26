package dev.pmlsp.dict.domain.model;

public enum ClaimStatus {
    /** Just created by the claimer participant. */
    OPEN,
    /** Donor participant has acknowledged; awaiting holder action. */
    WAITING_RESOLUTION,
    /** Donor has confirmed; ready to be completed by claimer. */
    CONFIRMED,
    /** Claim was completed — ownership/account moved to claimer. */
    COMPLETED,
    /** Claim was cancelled (by claimer, donor refusal, or timeout). */
    CANCELLED;

    public boolean isPending() {
        return this == OPEN || this == WAITING_RESOLUTION || this == CONFIRMED;
    }

    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }
}
