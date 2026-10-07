package dev.stefanjakic.lifecycle;

import dev.stefanjakic.lifecycle.application.OrderService;
import dev.stefanjakic.lifecycle.domain.InvalidOrderTransitionException;
import dev.stefanjakic.lifecycle.infrastructure.memory.InMemoryOrderRepository;

public final class LifecycleDemo {

    private LifecycleDemo() {
    }

    public static void main(String[] args) {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        OrderService service = new OrderService(repository);

        happyPath(service);
        protectedInvariant(service);
    }

    private static void happyPath(OrderService service) {
        String orderId = service.createOrder();

        service.confirmOrder(orderId);
        service.shipOrder(orderId);
        service.completeOrder(orderId);

        System.out.println("Happy path: " + service.statusOf(orderId));
    }

    private static void protectedInvariant(OrderService service) {
        String orderId = service.createOrder();

        try {
            service.shipOrder(orderId);
        } catch (InvalidOrderTransitionException exception) {
            System.out.println("Protected invariant: " + exception.getMessage());
        }
    }
}
