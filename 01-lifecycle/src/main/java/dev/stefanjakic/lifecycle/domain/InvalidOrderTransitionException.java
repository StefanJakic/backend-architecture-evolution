package dev.stefanjakic.lifecycle.domain;

public class InvalidOrderTransitionException extends RuntimeException {

    public InvalidOrderTransitionException(OrderStatus current, String action) {
        super("Cannot " + action + " order while status is " + current);
    }
}
