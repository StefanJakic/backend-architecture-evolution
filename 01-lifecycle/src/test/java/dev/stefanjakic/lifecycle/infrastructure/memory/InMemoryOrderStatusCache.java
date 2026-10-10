package dev.stefanjakic.lifecycle.infrastructure.memory;

import dev.stefanjakic.lifecycle.application.OrderStatusCache;
import dev.stefanjakic.lifecycle.domain.OrderStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryOrderStatusCache implements OrderStatusCache {

    private final Map<String, OrderStatus> statuses = new HashMap<>();

    @Override
    public Optional<OrderStatus> find(String orderId) {
        return Optional.ofNullable(statuses.get(orderId));
    }

    @Override
    public void put(String orderId, OrderStatus status) {
        statuses.put(orderId, status);
    }

    @Override
    public void invalidate(String orderId) {
        statuses.remove(orderId);
    }
}
