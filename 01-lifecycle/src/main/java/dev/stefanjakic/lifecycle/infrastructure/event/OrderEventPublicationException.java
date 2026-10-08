package dev.stefanjakic.lifecycle.infrastructure.event;

import java.util.UUID;

public final class OrderEventPublicationException extends RuntimeException {

    public OrderEventPublicationException(
        UUID eventId,
        Throwable cause
    ) {
        super("Failed to publish Order event " + eventId, cause);
    }
}
