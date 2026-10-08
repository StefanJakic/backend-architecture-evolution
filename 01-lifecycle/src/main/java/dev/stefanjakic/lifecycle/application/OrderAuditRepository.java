package dev.stefanjakic.lifecycle.application;

import dev.stefanjakic.lifecycle.domain.OrderStatus;

import java.util.List;

public interface OrderAuditRepository {

    void append(
        String orderId,
        OrderAuditAction action,
        OrderStatus fromStatus,
        OrderStatus toStatus
    );

    List<OrderAuditEntry> findByOrderId(String orderId);
}
