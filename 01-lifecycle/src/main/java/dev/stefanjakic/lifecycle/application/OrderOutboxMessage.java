package dev.stefanjakic.lifecycle.application;

import java.time.Instant;
import java.util.UUID;

public record OrderOutboxMessage(
    UUID eventId,
    String orderId,
    String eventType,
    String payload,
    Instant occurredAt,
    int attemptCount
) {
}
