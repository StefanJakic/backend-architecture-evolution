package dev.stefanjakic.lifecycle.application;

import dev.stefanjakic.lifecycle.domain.Order;
import dev.stefanjakic.lifecycle.domain.OrderStatus;

import java.util.UUID;
import java.util.function.Consumer;

public final class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public String createOrder() {
        Order order = new Order(UUID.randomUUID().toString());
        repository.save(order);
        return order.id();
    }

    public void confirmOrder(String orderId) {
        change(orderId, Order::confirm);
    }

    public void shipOrder(String orderId) {
        change(orderId, Order::ship);
    }

    public void completeOrder(String orderId) {
        change(orderId, Order::complete);
    }

    public void cancelOrder(String orderId) {
        change(orderId, Order::cancel);
    }

    public OrderStatus statusOf(String orderId) {
        return load(orderId).status();
    }

    private void change(String orderId, Consumer<Order> transition) {
        Order order = load(orderId);
        transition.accept(order);
        repository.save(order);
    }

    private Order load(String orderId) {
        return repository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
