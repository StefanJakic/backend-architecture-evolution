package dev.stefanjakic.lifecycle.application;

import dev.stefanjakic.lifecycle.domain.InvalidOrderTransitionException;
import dev.stefanjakic.lifecycle.domain.OrderStatus;
import dev.stefanjakic.lifecycle.infrastructure.memory.InMemoryOrderAuditRepository;
import dev.stefanjakic.lifecycle.infrastructure.memory.InMemoryOrderCreationIdempotencyRepository;
import dev.stefanjakic.lifecycle.infrastructure.memory.InMemoryOrderRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderServiceTest {

    private final InMemoryOrderAuditRepository auditRepository =
        new InMemoryOrderAuditRepository();

    private final OrderService service = new OrderService(
        new InMemoryOrderRepository(),
        new InMemoryOrderCreationIdempotencyRepository(),
        auditRepository
    );

    @Test
    void createsAndStoresOrderWithAuditEntry() {
        String orderId = service.createOrder("create-1");

        assertEquals(OrderStatus.CREATED, service.statusOf(orderId));

        var history = service.historyOf(orderId);

        assertEquals(1, history.size());
        assertEquals(OrderAuditAction.CREATE, history.get(0).action());
        assertNull(history.get(0).fromStatus());
        assertEquals(OrderStatus.CREATED, history.get(0).toStatus());
    }

    @Test
    void repeatedCreateKeyReturnsTheSameOrderWithoutDuplicateAudit() {
        String firstOrderId = service.createOrder("same-key");
        String retriedOrderId = service.createOrder("same-key");

        assertEquals(firstOrderId, retriedOrderId);
        assertEquals(1, service.historyOf(firstOrderId).size());
    }

    @Test
    void rejectsBlankIdempotencyKey() {
        assertThrows(
            InvalidIdempotencyKeyException.class,
            () -> service.createOrder(" ")
        );
    }

    @Test
    void recordsLifecycleHistoryInOrder() {
        String orderId = service.createOrder("lifecycle-1");

        service.confirmOrder(orderId);
        service.shipOrder(orderId);
        service.completeOrder(orderId);

        assertEquals(OrderStatus.COMPLETED, service.statusOf(orderId));

        var history = service.historyOf(orderId);

        assertEquals(4, history.size());
        assertEquals(OrderAuditAction.CREATE, history.get(0).action());
        assertEquals(OrderAuditAction.CONFIRM, history.get(1).action());
        assertEquals(OrderAuditAction.SHIP, history.get(2).action());
        assertEquals(OrderAuditAction.COMPLETE, history.get(3).action());
        assertEquals(OrderStatus.CREATED, history.get(1).fromStatus());
        assertEquals(OrderStatus.CONFIRMED, history.get(1).toStatus());
    }

    @Test
    void failedTransitionDoesNotCreateAuditEntry() {
        String orderId = service.createOrder("cancel-1");
        service.confirmOrder(orderId);
        service.shipOrder(orderId);

        assertThrows(
            InvalidOrderTransitionException.class,
            () -> service.cancelOrder(orderId)
        );

        assertEquals(OrderStatus.SHIPPED, service.statusOf(orderId));
        assertEquals(3, service.historyOf(orderId).size());
    }

    @Test
    void unknownOrderIsRejectedAtApplicationBoundary() {
        assertThrows(
            OrderNotFoundException.class,
            () -> service.confirmOrder("missing-order")
        );

        assertThrows(
            OrderNotFoundException.class,
            () -> service.historyOf("missing-order")
        );
    }
}
