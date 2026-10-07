package dev.stefanjakic.lifecycle.infrastructure.memory;

import dev.stefanjakic.lifecycle.application.OrderRepository;
import dev.stefanjakic.lifecycle.domain.Order;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> orders = new HashMap<>();

    @Override
    public Order save(Order order) {
        orders.put(order.id(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }
}
