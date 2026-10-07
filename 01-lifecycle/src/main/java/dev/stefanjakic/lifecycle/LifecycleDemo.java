package dev.stefanjakic.lifecycle;

import dev.stefanjakic.lifecycle.domain.InvalidOrderTransitionException;
import dev.stefanjakic.lifecycle.domain.Order;

public final class LifecycleDemo {

    private LifecycleDemo() {
    }

    public static void main(String[] args) {
        happyPath();
        protectedInvariant();
    }

    private static void happyPath() {
        Order order = new Order("ORDER-1001");
        order.confirm();
        order.ship();
        order.complete();

        System.out.println("Happy path: " + order.status());
    }

    private static void protectedInvariant() {
        Order order = new Order("ORDER-1002");

        try {
            order.ship();
        } catch (InvalidOrderTransitionException exception) {
            System.out.println("Protected invariant: " + exception.getMessage());
        }
    }
}
