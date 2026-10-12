package dev.stefanjakic.workflow.application;

public class WorkflowNotFoundException
    extends RuntimeException {

    public WorkflowNotFoundException(String orderId) {
        super("Workflow not found for order: " + orderId);
    }
}
