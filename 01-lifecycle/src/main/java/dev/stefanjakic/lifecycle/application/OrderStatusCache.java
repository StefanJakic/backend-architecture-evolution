package dev.stefanjakic.lifecycle.application;

import dev.stefanjakic.lifecycle.domain.OrderStatus;

import java.util.Optional;

public interface OrderStatusCache {

    Optional<OrderStatus> find(String orderId);

    void put(String orderId, OrderStatus status);

    void invalidate(String orderId);
}
