package dev.stefanjakic.lifecycle.infrastructure.persistence;

import dev.stefanjakic.lifecycle.application.OrderRepository;
import dev.stefanjakic.lifecycle.domain.Order;
import dev.stefanjakic.lifecycle.domain.OrderStatus;
import dev.stefanjakic.lifecycle.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

@Testcontainers
@SpringBootTest
class PostgresOrderRepositoryTest extends PostgresIntegrationTest {

    @Autowired
    private OrderRepository repository;

    @Test
    void restoresOrderStateFromPostgres() {
        Order order = new Order(UUID.randomUUID().toString());
        order.confirm();

        repository.save(order);

        Order restored = repository.findById(order.id()).orElseThrow();

        assertNotSame(order, restored);
        assertEquals(order.id(), restored.id());
        assertEquals(OrderStatus.CONFIRMED, restored.status());
    }
}
