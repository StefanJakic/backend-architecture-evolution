package dev.stefanjakic.lifecycle.application;

import dev.stefanjakic.lifecycle.domain.Order;

import java.util.Optional;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(String id);
}
