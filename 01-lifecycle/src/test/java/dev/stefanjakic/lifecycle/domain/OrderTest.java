package dev.stefanjakic.lifecycle.domain;

import dev.stefanjakic.lifecycle.domain.event.OrderConfirmed;
import dev.stefanjakic.lifecycle.domain.event.OrderCreated;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderTest {

    @Test
    void newOrderStartsCreatedAndRecordsCreationEvent() {
        Order order = new Order("ORDER-1");

        assertEquals(OrderStatus.CREATED, order.status());

        var events = order.releaseEvents();

        assertEquals(1, events.size());
        assertInstanceOf(OrderCreated.class, events.get(0));
        assertEquals("ORDER-1", events.get(0).orderId());
        assertEquals(OrderStatus.CREATED, events.get(0).toStatus());
    }

    @Test
    void restoredOrderDoesNotInventDomainEvents() {
        Order order = Order.restore("ORDER-RESTORED", OrderStatus.CONFIRMED);

        assertTrue(order.releaseEvents().isEmpty());
    }

    @Test
    void successfulTransitionRecordsBusinessEvent() {
        Order order = new Order("ORDER-2");
        order.releaseEvents();

        order.confirm();

        var events = order.releaseEvents();

        assertEquals(1, events.size());
        assertInstanceOf(OrderConfirmed.class, events.get(0));
        assertEquals(OrderStatus.CREATED, events.get(0).fromStatus());
        assertEquals(OrderStatus.CONFIRMED, events.get(0).toStatus());
    }

    @Test
    void followsHappyPathToCompletion() {
        Order order = new Order("ORDER-3");

        order.confirm();
        order.ship();
        order.complete();

        assertEquals(OrderStatus.COMPLETED, order.status());
    }

    @Test
    void createdOrderCanBeCancelled() {
        Order order = new Order("ORDER-4");

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.status());
    }

    @Test
    void confirmedOrderCanBeCancelled() {
        Order order = new Order("ORDER-5");
        order.confirm();

        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.status());
    }

    @Test
    void invalidTransitionDoesNotRecordEvent() {
        Order order = new Order("ORDER-6");
        order.releaseEvents();

        assertThrows(InvalidOrderTransitionException.class, order::ship);
        assertEquals(OrderStatus.CREATED, order.status());
        assertTrue(order.releaseEvents().isEmpty());
    }

    @Test
    void cannotCancelCompletedOrder() {
        Order order = new Order("ORDER-7");
        order.confirm();
        order.ship();
        order.complete();

        assertThrows(InvalidOrderTransitionException.class, order::cancel);
        assertEquals(OrderStatus.COMPLETED, order.status());
    }
}
