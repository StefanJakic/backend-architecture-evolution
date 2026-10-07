package dev.stefanjakic.lifecycle.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    @Test
    void newOrderStartsCreated() {
        Order order = new Order("ORDER-1");

        assertEquals(OrderStatus.CREATED, order.status());
    }

    @Test
    void followsHappyPathToCompletion() {
        Order order = new Order("ORDER-2");

        order.confirm();
        order.ship();
        order.complete();

        assertEquals(OrderStatus.COMPLETED, order.status());
    }

    @Test
    void createdOrderCanBeCancelled() {
        Order order = new Order("ORDER-3");

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.status());
    }

    @Test
    void confirmedOrderCanBeCancelled() {
        Order order = new Order("ORDER-4");
        order.confirm();

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.status());
    }

    @Test
    void cannotShipCreatedOrder() {
        Order order = new Order("ORDER-5");

        assertThrows(InvalidOrderTransitionException.class, order::ship);
        assertEquals(OrderStatus.CREATED, order.status());
    }

    @Test
    void cannotCancelCompletedOrder() {
        Order order = new Order("ORDER-6");
        order.confirm();
        order.ship();
        order.complete();

        assertThrows(InvalidOrderTransitionException.class, order::cancel);
        assertEquals(OrderStatus.COMPLETED, order.status());
    }
}
