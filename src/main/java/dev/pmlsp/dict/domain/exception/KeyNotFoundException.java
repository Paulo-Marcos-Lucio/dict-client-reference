package dev.pmlsp.dict.domain.exception;

import dev.pmlsp.dict.domain.model.PixKey;

public class KeyNotFoundException extends DictException {

    private final PixKey key;

    public KeyNotFoundException(PixKey key) {
        super("DICT entry not found for key " + key.masked());
        this.key = key;
    }

    public PixKey key() {
        return key;
    }
}
