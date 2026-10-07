package dev.stefanjakic.lifecycle.infrastructure.configuration;

import dev.stefanjakic.lifecycle.application.OrderRepository;
import dev.stefanjakic.lifecycle.application.OrderService;
import dev.stefanjakic.lifecycle.infrastructure.memory.InMemoryOrderRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationConfiguration {

    @Bean
    OrderRepository orderRepository() {
        return new InMemoryOrderRepository();
    }

    @Bean
    OrderService orderService(OrderRepository repository) {
        return new OrderService(repository);
    }
}
