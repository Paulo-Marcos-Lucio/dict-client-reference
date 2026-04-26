package dev.pmlsp.dict.domain.exception;

/**
 * Base type for any error raised by the DICT client domain or its adapters.
 * Subclasses correspond to taxonomized failures the BCB DICT API may surface.
 */
public abstract class DictException extends RuntimeException {

    protected DictException(String message) {
        super(message);
    }

    protected DictException(String message, Throwable cause) {
        super(message, cause);
    }
}
