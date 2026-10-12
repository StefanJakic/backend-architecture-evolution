package dev.stefanjakic.workflow.application;

import dev.stefanjakic.workflow.domain.FulfillmentCommand;
import dev.stefanjakic.workflow.domain.FulfillmentState;

import java.util.Optional;

public interface FulfillmentStateRepository {

    void createIfAbsent(FulfillmentCommand command);

    Optional<FulfillmentState> findByOrderId(String orderId);

    void markInventoryReserved(
        String orderId,
        String reservationId
    );

    void markPaymentCharged(
        String orderId,
        String paymentId
    );

    void markCompleted(
        String orderId,
        String shipmentId
    );

    void markCompensated(String orderId);

    void markCompensationFailed(String orderId);
}
