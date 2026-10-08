package dev.stefanjakic.lifecycle.application;

public interface OrderEventTransport {

    void publish(OrderOutboxMessage message);
}
