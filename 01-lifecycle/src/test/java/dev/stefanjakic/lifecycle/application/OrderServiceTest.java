package dev.stefanjakic.lifecycle.application;

import dev.stefanjakic.lifecycle.domain.InvalidOrderTransitionException;
import dev.stefanjakic.lifecycle.domain.OrderStatus;
import dev.stefanjakic.lifecycle.infrastructure.memory.InMemoryOrderRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderServiceTest {

    private final OrderService service =
        new OrderService(new InMemoryOrderRepository());

    @Test
    void createsAndStoresOrder() {
        String orderId = service.createOrder();

        assertEquals(OrderStatus.CREATED, service.statusOf(orderId));
    }

    @Test
    void executesLifecycleUseCasesById() {
        String orderId = service.createOrder();

        service.confirmOrder(orderId);
        service.shipOrder(orderId);
        service.completeOrder(orderId);

        assertEquals(OrderStatus.COMPLETED, service.statusOf(orderId));
    }

    @Test
    void cancellationStillUsesDomainRules() {
        String orderId = service.createOrder();
        service.confirmOrder(orderId);
        service.shipOrder(orderId);

        assertThrows(
            InvalidOrderTransitionException.class,
            () -> service.cancelOrder(orderId)
        );

        assertEquals(OrderStatus.SHIPPED, service.statusOf(orderId));
    }

    @Test
    void unknownOrderIsRejectedAtApplicationBoundary() {
        assertThrows(
            OrderNotFoundException.class,
            () -> service.confirmOrder("missing-order")
        );
    }
}
