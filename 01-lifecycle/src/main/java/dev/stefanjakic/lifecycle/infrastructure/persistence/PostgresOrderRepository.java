package dev.stefanjakic.lifecycle.infrastructure.persistence;

import dev.stefanjakic.lifecycle.application.ConcurrentOrderModificationException;
import dev.stefanjakic.lifecycle.application.OrderRepository;
import dev.stefanjakic.lifecycle.domain.Order;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PostgresOrderRepository implements OrderRepository {

    private final SpringDataOrderRepository repository;

    public PostgresOrderRepository(SpringDataOrderRepository repository) {
        this.repository = repository;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = repository.findById(order.id())
            .map(existing -> {
                existing.changeStatus(order.status());
                return existing;
            })
            .orElseGet(() -> new OrderJpaEntity(order.id(), order.status()));

        try {
            repository.saveAndFlush(entity);
            return order;
        } catch (OptimisticLockingFailureException exception) {
            throw new ConcurrentOrderModificationException(order.id(), exception);
        }
    }

    @Override
    public Optional<Order> findById(String id) {
        return repository.findById(id)
            .map(entity -> Order.restore(entity.id(), entity.status()));
    }
}
