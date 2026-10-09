package dev.stefanjakic.lifecycle.infrastructure.configuration;

import dev.stefanjakic.lifecycle.application.OrderAuditRepository;
import dev.stefanjakic.lifecycle.application.OrderCreationIdempotencyRepository;
import dev.stefanjakic.lifecycle.application.OrderEventTransport;
import dev.stefanjakic.lifecycle.application.OrderOutboxRelay;
import dev.stefanjakic.lifecycle.application.OrderOutboxRelayObserver;
import dev.stefanjakic.lifecycle.application.OrderOutboxRelayRepository;
import dev.stefanjakic.lifecycle.application.OrderOutboxRepository;
import dev.stefanjakic.lifecycle.application.OrderRepository;
import dev.stefanjakic.lifecycle.application.OrderService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    OrderService orderService(
        OrderRepository repository,
        OrderCreationIdempotencyRepository idempotencyRepository,
        OrderAuditRepository auditRepository,
        OrderOutboxRepository outboxRepository
    ) {
        return new OrderService(
            repository,
            idempotencyRepository,
            auditRepository,
            outboxRepository
        );
    }

    @Bean
    OrderOutboxRelay orderOutboxRelay(
        OrderOutboxRelayRepository repository,
        OrderEventTransport transport,
        OrderOutboxRelayObserver observer,
        @Value("${outbox.relay.batch-size:100}") int batchSize,
        @Value("${outbox.relay.lease-timeout-ms:30000}") long leaseTimeoutMs
    ) {
        return new OrderOutboxRelay(
            repository,
            transport,
            observer,
            batchSize,
            Duration.ofMillis(leaseTimeoutMs)
        );
    }
}
