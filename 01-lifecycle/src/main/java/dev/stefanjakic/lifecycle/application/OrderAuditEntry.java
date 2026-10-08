package dev.stefanjakic.lifecycle.application;

import dev.stefanjakic.lifecycle.domain.OrderStatus;

import java.time.Instant;

public record OrderAuditEntry(
    long id,
    String orderId,
    OrderAuditAction action,
    OrderStatus fromStatus,
    OrderStatus toStatus,
    Instant occurredAt
) {
}
