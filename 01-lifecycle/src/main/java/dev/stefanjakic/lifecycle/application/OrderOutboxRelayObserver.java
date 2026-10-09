package dev.stefanjakic.lifecycle.application;

public interface OrderOutboxRelayObserver {

    void published(OrderOutboxMessage message);

    void failed(OrderOutboxMessage message, RuntimeException exception);
}
