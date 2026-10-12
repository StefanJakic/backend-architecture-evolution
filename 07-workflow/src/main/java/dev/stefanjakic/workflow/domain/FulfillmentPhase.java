package dev.stefanjakic.workflow.domain;

public enum FulfillmentPhase {
    STARTED,
    INVENTORY_RESERVED,
    PAYMENT_CHARGED,
    COMPLETED,
    COMPENSATED,
    COMPENSATION_FAILED
}
