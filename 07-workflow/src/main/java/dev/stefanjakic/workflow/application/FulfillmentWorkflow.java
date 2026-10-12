package dev.stefanjakic.workflow.application;

import dev.stefanjakic.workflow.domain.FulfillmentCommand;
import dev.stefanjakic.workflow.domain.FulfillmentResult;
import dev.stefanjakic.workflow.domain.FulfillmentState;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class FulfillmentWorkflow {

    private final InventoryReservation inventoryReservation;
    private final PaymentCharging paymentCharging;
    private final ShipmentCreation shipmentCreation;
    private final FulfillmentStateRepository stateRepository;

    public FulfillmentWorkflow(
        InventoryReservation inventoryReservation,
        PaymentCharging paymentCharging,
        ShipmentCreation shipmentCreation,
        FulfillmentStateRepository stateRepository
    ) {
        this.inventoryReservation = Objects.requireNonNull(
            inventoryReservation,
            "inventoryReservation must not be null"
        );

        this.paymentCharging = Objects.requireNonNull(
            paymentCharging,
            "paymentCharging must not be null"
        );

        this.shipmentCreation = Objects.requireNonNull(
            shipmentCreation,
            "shipmentCreation must not be null"
        );

        this.stateRepository = Objects.requireNonNull(
            stateRepository,
            "stateRepository must not be null"
        );
    }

    public FulfillmentResult fulfill(
        FulfillmentCommand command
    ) {
        Objects.requireNonNull(
            command,
            "command must not be null"
        );

        stateRepository.createIfAbsent(command);

        FulfillmentState state =
            load(command.orderId());

        if (state.amountCents() != command.amountCents()) {
            throw new IllegalArgumentException(
                "Existing workflow amount differs for order: "
                    + command.orderId()
            );
        }

        return resume(command.orderId());
    }

    public FulfillmentResult resume(String orderId) {
        while (true) {
            FulfillmentState state = load(orderId);

            switch (state.phase()) {
                case STARTED -> reserveInventory(state);
                case INVENTORY_RESERVED ->
                    chargePayment(state);
                case PAYMENT_CHARGED ->
                    createShipment(state);
                case COMPLETED -> {
                    return state.result();
                }
                case COMPENSATED, COMPENSATION_FAILED ->
                    throw new WorkflowNotResumableException(
                        state.orderId(),
                        state.phase()
                    );
            }
        }
    }

    private void reserveInventory(
        FulfillmentState state
    ) {
        String reservationId =
            inventoryReservation.reserve(
                state.orderId(),
                operationKey(
                    state.orderId(),
                    "reserve"
                )
            );

        stateRepository.markInventoryReserved(
            state.orderId(),
            reservationId
        );
    }

    private void chargePayment(
        FulfillmentState state
    ) {
        String paymentId;

        try {
            paymentId = paymentCharging.charge(
                state.orderId(),
                state.amountCents(),
                operationKey(
                    state.orderId(),
                    "charge"
                )
            );
        } catch (RuntimeException failure) {
            compensateReservation(
                state,
                failure
            );
            throw failure;
        }

        stateRepository.markPaymentCharged(
            state.orderId(),
            paymentId
        );
    }

    private void createShipment(
        FulfillmentState state
    ) {
        String shipmentId;

        try {
            shipmentId =
                shipmentCreation.createShipment(
                    state.orderId(),
                    state.reservationId(),
                    state.paymentId(),
                    operationKey(
                        state.orderId(),
                        "shipment"
                    )
                );
        } catch (RuntimeException failure) {
            compensatePaymentAndReservation(
                state,
                failure
            );
            throw failure;
        }

        stateRepository.markCompleted(
            state.orderId(),
            shipmentId
        );
    }

    private void compensateReservation(
        FulfillmentState state,
        RuntimeException originalFailure
    ) {
        boolean compensationFailed =
            !compensate(
                originalFailure,
                () -> inventoryReservation.release(
                    state.reservationId()
                )
            );

        checkpointCompensation(
            state.orderId(),
            compensationFailed,
            originalFailure
        );
    }

    private void compensatePaymentAndReservation(
        FulfillmentState state,
        RuntimeException originalFailure
    ) {
        boolean compensationFailed = false;

        if (!compensate(
            originalFailure,
            () -> paymentCharging.refund(
                state.paymentId()
            )
        )) {
            compensationFailed = true;
        }

        if (!compensate(
            originalFailure,
            () -> inventoryReservation.release(
                state.reservationId()
            )
        )) {
            compensationFailed = true;
        }

        checkpointCompensation(
            state.orderId(),
            compensationFailed,
            originalFailure
        );
    }

    private void checkpointCompensation(
        String orderId,
        boolean compensationFailed,
        RuntimeException originalFailure
    ) {
        try {
            if (compensationFailed) {
                stateRepository.markCompensationFailed(
                    orderId
                );
            } else {
                stateRepository.markCompensated(
                    orderId
                );
            }
        } catch (RuntimeException checkpointFailure) {
            originalFailure.addSuppressed(
                checkpointFailure
            );
        }
    }

    private boolean compensate(
        RuntimeException originalFailure,
        Runnable compensation
    ) {
        try {
            compensation.run();
            return true;
        } catch (RuntimeException compensationFailure) {
            originalFailure.addSuppressed(
                compensationFailure
            );
            return false;
        }
    }

    private String operationKey(
        String orderId,
        String operation
    ) {
        return orderId + ":" + operation;
    }

    private FulfillmentState load(String orderId) {
        return stateRepository.findByOrderId(orderId)
            .orElseThrow(
                () -> new WorkflowNotFoundException(
                    orderId
                )
            );
    }
}
