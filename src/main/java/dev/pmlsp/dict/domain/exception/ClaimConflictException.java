package dev.pmlsp.dict.domain.exception;

import dev.pmlsp.dict.domain.model.PixKey;

/**
 * A claim already exists for the given key (DICT only allows one open claim per key).
 */
public class ClaimConflictException extends DictException {

    private final PixKey key;

    public ClaimConflictException(PixKey key) {
        super("DICT already has an open claim for key " + key.masked());
        this.key = key;
    }

    public PixKey key() {
        return key;
    }
}
