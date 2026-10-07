package dev.stefanjakic.lifecycle.api;

import dev.stefanjakic.lifecycle.domain.OrderStatus;

public record OrderResponse(
    String id,
    String status
) {

    static OrderResponse of(String id, OrderStatus status) {
        return new OrderResponse(id, status.name());
    }
}
