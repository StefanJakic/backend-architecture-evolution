package dev.stefanjakic.lifecycle.application;

public final class IdempotencyResultUnavailableException extends RuntimeException {

    public IdempotencyResultUnavailableException(String idempotencyKey) {
        super("Idempotency result is not available for key: " + idempotencyKey);
    }
}
