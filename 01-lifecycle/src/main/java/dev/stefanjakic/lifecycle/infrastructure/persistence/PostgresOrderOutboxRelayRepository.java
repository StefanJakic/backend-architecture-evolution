package dev.stefanjakic.lifecycle.infrastructure.persistence;

import dev.stefanjakic.lifecycle.application.OrderOutboxMessage;
import dev.stefanjakic.lifecycle.application.OrderOutboxRelayRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public class PostgresOrderOutboxRelayRepository
    implements OrderOutboxRelayRepository {

    private static final int MAX_ERROR_LENGTH = 2000;

    private final JdbcTemplate jdbcTemplate;

    public PostgresOrderOutboxRelayRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public List<OrderOutboxMessage> claimBatch(
        int batchSize,
        Duration leaseTimeout
    ) {
        Instant leaseExpiredBefore = Instant.now().minus(leaseTimeout);

        return jdbcTemplate.query(
            """
            WITH candidates AS (
                SELECT event_id
                FROM order_event_outbox
                WHERE status = 'PENDING'
                   OR (
                        status = 'IN_PROGRESS'
                        AND claimed_at < ?
                   )
                ORDER BY recorded_at, event_id
                FOR UPDATE SKIP LOCKED
                LIMIT ?
            )
            UPDATE order_event_outbox outbox
            SET status = 'IN_PROGRESS',
                claimed_at = NOW(),
                published_at = NULL,
                attempt_count = outbox.attempt_count + 1,
                last_error = NULL
            FROM candidates
            WHERE outbox.event_id = candidates.event_id
            RETURNING
                outbox.event_id,
                outbox.order_id,
                outbox.event_type,
                outbox.payload::text,
                outbox.occurred_at,
                outbox.attempt_count
            """,
            (resultSet, rowNumber) -> new OrderOutboxMessage(
                resultSet.getObject("event_id", UUID.class),
                resultSet.getString("order_id"),
                resultSet.getString("event_type"),
                resultSet.getString("payload"),
                resultSet.getTimestamp("occurred_at").toInstant(),
                resultSet.getInt("attempt_count")
            ),
            Timestamp.from(leaseExpiredBefore),
            batchSize
        );
    }

    @Override
    public void markPublished(UUID eventId) {
        int updated = jdbcTemplate.update(
            """
            UPDATE order_event_outbox
            SET status = 'PUBLISHED',
                published_at = NOW(),
                last_error = NULL
            WHERE event_id = ?
              AND status = 'IN_PROGRESS'
            """,
            eventId
        );

        if (updated != 1) {
            throw new IllegalStateException(
                "Expected one in-progress outbox record for event: " + eventId
            );
        }
    }

    @Override
    public void releaseForRetry(UUID eventId, String errorMessage) {
        int updated = jdbcTemplate.update(
            """
            UPDATE order_event_outbox
            SET status = 'PENDING',
                claimed_at = NULL,
                last_error = ?
            WHERE event_id = ?
              AND status = 'IN_PROGRESS'
            """,
            truncate(errorMessage),
            eventId
        );

        if (updated != 1) {
            throw new IllegalStateException(
                "Expected one in-progress outbox record for event: " + eventId
            );
        }
    }

    private String truncate(String message) {
        if (message == null) {
            return null;
        }

        return message.length() <= MAX_ERROR_LENGTH
            ? message
            : message.substring(0, MAX_ERROR_LENGTH);
    }
}
