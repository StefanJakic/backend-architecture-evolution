package dev.stefanjakic.lifecycle.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class PostgresIntegrationTest {

    protected static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("lifecycle")
            .withUsername("lifecycle")
            .withPassword("lifecycle");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void integrationTestProperties(
        DynamicPropertyRegistry registry
    ) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);

        registry.add("order.status-cache.enabled", () -> "false");
        registry.add("outbox.relay.enabled", () -> "false");
        registry.add(
            "management.otlp.metrics.export.enabled",
            () -> "false"
        );
        registry.add(
            "management.tracing.export.otlp.enabled",
            () -> "false"
        );
    }
}
