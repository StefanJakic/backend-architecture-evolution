package dev.stefanjakic.lifecycle.infrastructure.persistence;

import dev.stefanjakic.lifecycle.domain.OrderStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orders")
public class OrderJpaEntity {

    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OrderStatus status;

    protected OrderJpaEntity() {
    }

    OrderJpaEntity(String id, OrderStatus status) {
        this.id = id;
        this.status = status;
    }

    String id() {
        return id;
    }

    OrderStatus status() {
        return status;
    }

    void changeStatus(OrderStatus status) {
        this.status = status;
    }
}
