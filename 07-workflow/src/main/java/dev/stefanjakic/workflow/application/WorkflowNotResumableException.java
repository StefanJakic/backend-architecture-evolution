package dev.stefanjakic.workflow.application;

import dev.stefanjakic.workflow.domain.FulfillmentPhase;

public class WorkflowNotResumableException
    extends RuntimeException {

    public WorkflowNotResumableException(
        String orderId,
        FulfillmentPhase phase
    ) {
        super(
            "Workflow cannot resume for order "
                + orderId
                + " in phase "
                + phase
        );
    }
}
