package dev.stefanjakic.lifecycle.infrastructure.configuration;

import dev.stefanjakic.lifecycle.application.OrderAuditRepository;
import dev.stefanjakic.lifecycle.application.OrderCreationIdempotencyRepository;
import dev.stefanjakic.lifecycle.application.OrderRepository;
import dev.stefanjakic.lifecycle.application.OrderService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    OrderService orderService(
        OrderRepository repository,
        OrderCreationIdempotencyRepository idempotencyRepository,
        OrderAuditRepository auditRepository
    ) {
        return new OrderService(
            repository,
            idempotencyRepository,
            auditRepository
        );
    }
}
