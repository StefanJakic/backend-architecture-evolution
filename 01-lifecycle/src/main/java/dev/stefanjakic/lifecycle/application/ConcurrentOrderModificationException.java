package dev.stefanjakic.lifecycle.application;

public final class ConcurrentOrderModificationException extends RuntimeException {

    public ConcurrentOrderModificationException(String orderId, Throwable cause) {
        super("Order was modified by another request: " + orderId, cause);
    }
}
