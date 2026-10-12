package dev.stefanjakic.workflow.infrastructure.persistence;

import dev.stefanjakic.workflow.application.FulfillmentStateRepository;
import dev.stefanjakic.workflow.domain.FulfillmentCommand;
import dev.stefanjakic.workflow.domain.FulfillmentPhase;
import dev.stefanjakic.workflow.domain.FulfillmentState;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PostgresFulfillmentStateRepository
    implements FulfillmentStateRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresFulfillmentStateRepository(
        JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void createIfAbsent(FulfillmentCommand command) {
        jdbcTemplate.update(
            """
            INSERT INTO fulfillment_workflow (
                order_id,
                amount_cents,
                phase
            )
            VALUES (?, ?, 'STARTED')
            ON CONFLICT (order_id) DO NOTHING
            """,
            command.orderId(),
            command.amountCents()
        );
    }

    @Override
    public Optional<FulfillmentState> findByOrderId(
        String orderId
    ) {
        return jdbcTemplate.query(
            """
            SELECT
                order_id,
                amount_cents,
                phase,
                reservation_id,
                payment_id,
                shipment_id
            FROM fulfillment_workflow
            WHERE order_id = ?
            """,
            resultSet -> {
                if (!resultSet.next()) {
                    return Optional.empty();
                }

                return Optional.of(
                    new FulfillmentState(
                        resultSet.getString("order_id"),
                        resultSet.getLong("amount_cents"),
                        FulfillmentPhase.valueOf(
                            resultSet.getString("phase")
                        ),
                        resultSet.getString("reservation_id"),
                        resultSet.getString("payment_id"),
                        resultSet.getString("shipment_id")
                    )
                );
            },
            orderId
        );
    }

    @Override
    public void markInventoryReserved(
        String orderId,
        String reservationId
    ) {
        int updated = jdbcTemplate.update(
            """
            UPDATE fulfillment_workflow
            SET phase = 'INVENTORY_RESERVED',
                reservation_id = ?
            WHERE order_id = ?
              AND phase = 'STARTED'
            """,
            reservationId,
            orderId
        );

        requireTransition(
            updated,
            orderId,
            FulfillmentPhase.INVENTORY_RESERVED
        );
    }

    @Override
    public void markPaymentCharged(
        String orderId,
        String paymentId
    ) {
        int updated = jdbcTemplate.update(
            """
            UPDATE fulfillment_workflow
            SET phase = 'PAYMENT_CHARGED',
                payment_id = ?
            WHERE order_id = ?
              AND phase = 'INVENTORY_RESERVED'
            """,
            paymentId,
            orderId
        );

        requireTransition(
            updated,
            orderId,
            FulfillmentPhase.PAYMENT_CHARGED
        );
    }

    @Override
    public void markCompleted(
        String orderId,
        String shipmentId
    ) {
        int updated = jdbcTemplate.update(
            """
            UPDATE fulfillment_workflow
            SET phase = 'COMPLETED',
                shipment_id = ?
            WHERE order_id = ?
              AND phase = 'PAYMENT_CHARGED'
            """,
            shipmentId,
            orderId
        );

        requireTransition(
            updated,
            orderId,
            FulfillmentPhase.COMPLETED
        );
    }

    @Override
    public void markCompensated(String orderId) {
        int updated = jdbcTemplate.update(
            """
            UPDATE fulfillment_workflow
            SET phase = 'COMPENSATED'
            WHERE order_id = ?
              AND phase IN (
                  'INVENTORY_RESERVED',
                  'PAYMENT_CHARGED'
              )
            """,
            orderId
        );

        requireTransition(
            updated,
            orderId,
            FulfillmentPhase.COMPENSATED
        );
    }

    @Override
    public void markCompensationFailed(String orderId) {
        int updated = jdbcTemplate.update(
            """
            UPDATE fulfillment_workflow
            SET phase = 'COMPENSATION_FAILED'
            WHERE order_id = ?
              AND phase IN (
                  'INVENTORY_RESERVED',
                  'PAYMENT_CHARGED'
              )
            """,
            orderId
        );

        requireTransition(
            updated,
            orderId,
            FulfillmentPhase.COMPENSATION_FAILED
        );
    }

    private void requireTransition(
        int updated,
        String orderId,
        FulfillmentPhase targetPhase
    ) {
        if (updated != 1) {
            throw new IllegalStateException(
                "Cannot move workflow "
                    + orderId
                    + " to "
                    + targetPhase
            );
        }
    }
}
