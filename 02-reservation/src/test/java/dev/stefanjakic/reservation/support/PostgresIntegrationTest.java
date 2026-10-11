package dev.stefanjakic.reservation.support;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

public abstract class PostgresIntegrationTest {

    protected static final PostgreSQLContainer POSTGRES =
        new PostgreSQLContainer("postgres:17-alpine")
            .withDatabaseName("reservation")
            .withUsername("reservation")
            .withPassword("reservation");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void postgresProperties(
        DynamicPropertyRegistry registry
    ) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
