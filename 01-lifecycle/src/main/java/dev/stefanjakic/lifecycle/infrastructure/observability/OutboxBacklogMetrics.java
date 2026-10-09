package dev.stefanjakic.lifecycle.infrastructure.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class OutboxBacklogMetrics {

    private final JdbcTemplate jdbcTemplate;

    public OutboxBacklogMetrics(
        MeterRegistry registry,
        JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;

        Gauge.builder(
                "order.outbox.backlog",
                this,
                metrics -> metrics.count("PENDING")
            )
            .description("Current number of outbox rows by delivery state")
            .tag("status", "pending")
            .register(registry);

        Gauge.builder(
                "order.outbox.backlog",
                this,
                metrics -> metrics.count("IN_PROGRESS")
            )
            .description("Current number of outbox rows by delivery state")
            .tag("status", "in_progress")
            .register(registry);

        Gauge.builder(
                "order.outbox.oldest.unpublished.age",
                this,
                OutboxBacklogMetrics::oldestUnpublishedAgeSeconds
            )
            .description("Age in seconds of the oldest unpublished outbox event")
            .baseUnit("seconds")
            .register(registry);
    }

    private double count(String status) {
        Long count = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
            FROM order_event_outbox
            WHERE status = ?
            """,
            Long.class,
            status
        );

        return count == null ? 0 : count.doubleValue();
    }

    private double oldestUnpublishedAgeSeconds() {
        Double age = jdbcTemplate.queryForObject(
            """
            SELECT COALESCE(
                EXTRACT(EPOCH FROM (NOW() - MIN(recorded_at))),
                0
            )
            FROM order_event_outbox
            WHERE status <> 'PUBLISHED'
            """,
            Double.class
        );

        return age == null ? 0 : Math.max(age, 0);
    }
}
