package dev.stefanjakic.lifecycle.application;

import dev.stefanjakic.lifecycle.domain.Order;
import dev.stefanjakic.lifecycle.domain.OrderStatus;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Transactional
public class OrderService {

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
        Order order = load(orderId);
        order.confirm();
        repository.save(order);
    }

    public void shipOrder(String orderId) {
        Order order = load(orderId);
        order.ship();
        repository.save(order);
    }

    public void completeOrder(String orderId) {
        Order order = load(orderId);
        order.complete();
        repository.save(order);
    }

    public void cancelOrder(String orderId) {
        Order order = load(orderId);
        order.cancel();
        repository.save(order);
    }

    @Transactional(readOnly = true)
    public OrderStatus statusOf(String orderId) {
        return load(orderId).status();
    }

    private Order load(String orderId) {
        return repository.findById(orderId)
            .orElseThrow(() -> new OrderNotFoundException(orderId));
    }
}
