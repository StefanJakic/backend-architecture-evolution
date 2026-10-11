package dev.stefanjakic.assignment.application;

public class NoAssignableJobException extends RuntimeException {

    public NoAssignableJobException(String capability) {
        super("No pending job for capability: " + capability);
    }
}
