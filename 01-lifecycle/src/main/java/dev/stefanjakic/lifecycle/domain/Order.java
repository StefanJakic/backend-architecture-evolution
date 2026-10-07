package dev.stefanjakic.lifecycle.domain;

import java.util.Objects;

public final class Order {

    private final String id;
    private OrderStatus status;

    public Order(String id) {
        this.id = Objects.requireNonNull(id, "id");
        this.status = OrderStatus.CREATED;
    }

    public String id() {
        return id;
    }

    public OrderStatus status() {
        return status;
    }

    public void confirm() {
        require(OrderStatus.CREATED, "confirm");
        status = OrderStatus.CONFIRMED;
    }

    public void ship() {
        require(OrderStatus.CONFIRMED, "ship");
        status = OrderStatus.SHIPPED;
    }

    public void complete() {
        require(OrderStatus.SHIPPED, "complete");
        status = OrderStatus.COMPLETED;
    }

    public void cancel() {
        if (status != OrderStatus.CREATED && status != OrderStatus.CONFIRMED) {
            throw new InvalidOrderTransitionException(status, "cancel");
        }
        status = OrderStatus.CANCELLED;
    }

    private void require(OrderStatus expected, String action) {
        if (status != expected) {
            throw new InvalidOrderTransitionException(status, action);
        }
    }
}
