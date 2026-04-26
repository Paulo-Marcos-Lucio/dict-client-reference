package dev.pmlsp.dict.domain.exception;

import dev.pmlsp.dict.domain.model.PixKey;

public class DuplicateKeyException extends DictException {

    private final PixKey key;

    public DuplicateKeyException(PixKey key) {
        super("DICT key already registered: " + key.masked());
        this.key = key;
    }

    public PixKey key() {
        return key;
    }
}
