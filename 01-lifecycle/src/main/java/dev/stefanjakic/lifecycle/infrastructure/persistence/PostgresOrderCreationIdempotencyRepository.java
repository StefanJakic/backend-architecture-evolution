package dev.stefanjakic.lifecycle.infrastructure.persistence;

import dev.stefanjakic.lifecycle.application.OrderCreationIdempotencyRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PostgresOrderCreationIdempotencyRepository
    implements OrderCreationIdempotencyRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresOrderCreationIdempotencyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<String> findCompletedOrderId(String idempotencyKey) {
        return jdbcTemplate.query(
            """
            SELECT order_id
            FROM order_creation_idempotency
            WHERE idempotency_key = ?
              AND status = 'COMPLETED'
            """,
            resultSet -> resultSet.next()
                ? Optional.of(resultSet.getString("order_id"))
                : Optional.empty(),
            idempotencyKey
        );
    }

    @Override
    public boolean tryClaim(String idempotencyKey) {
        int inserted = jdbcTemplate.update(
            """
            INSERT INTO order_creation_idempotency (idempotency_key, status)
            VALUES (?, 'IN_PROGRESS')
            ON CONFLICT (idempotency_key) DO NOTHING
            """,
            idempotencyKey
        );

        return inserted == 1;
    }

    @Override
    public void complete(String idempotencyKey, String orderId) {
        int updated = jdbcTemplate.update(
            """
            UPDATE order_creation_idempotency
            SET status = 'COMPLETED',
                order_id = ?,
                completed_at = NOW()
            WHERE idempotency_key = ?
              AND status = 'IN_PROGRESS'
            """,
            orderId,
            idempotencyKey
        );

        if (updated != 1) {
            throw new IllegalStateException(
                "Expected one in-progress idempotency record for key: "
                    + idempotencyKey
            );
        }
    }
}
