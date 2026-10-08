package dev.stefanjakic.lifecycle.api;

import dev.stefanjakic.lifecycle.application.OrderAuditEntry;

import java.time.Instant;

public record OrderAuditResponse(
    long id,
    String action,
    String fromStatus,
    String toStatus,
    Instant occurredAt
) {

    static OrderAuditResponse of(OrderAuditEntry entry) {
        return new OrderAuditResponse(
            entry.id(),
            entry.action().name(),
            entry.fromStatus() == null ? null : entry.fromStatus().name(),
            entry.toStatus().name(),
            entry.occurredAt()
        );
    }
}
