package dev.pmlsp.dict.domain.exception;

import java.util.UUID;

public class ClaimNotFoundException extends DictException {

    private final UUID claimId;

    public ClaimNotFoundException(UUID claimId) {
        super("DICT claim not found: " + claimId);
        this.claimId = claimId;
    }

    public UUID claimId() {
        return claimId;
    }
}
