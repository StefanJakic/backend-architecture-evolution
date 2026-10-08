package dev.stefanjakic.lifecycle.infrastructure.event;

import dev.stefanjakic.lifecycle.application.OrderEventTransport;
import dev.stefanjakic.lifecycle.application.OrderOutboxMessage;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class KafkaOrderEventTransport implements OrderEventTransport {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final long publishTimeoutSeconds;

    public KafkaOrderEventTransport(
        KafkaTemplate<String, String> kafkaTemplate,
        @Value("${order.events.topic:order.lifecycle.v1}") String topic,
        @Value("${outbox.relay.publish-timeout-seconds:10}")
        long publishTimeoutSeconds
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.publishTimeoutSeconds = publishTimeoutSeconds;
    }

    @Override
    public void publish(OrderOutboxMessage message) {
        ProducerRecord<String, String> record = new ProducerRecord<>(
            topic,
            message.orderId(),
            message.payload()
        );

        record.headers().add(
            "eventId",
            message.eventId().toString().getBytes(StandardCharsets.UTF_8)
        );
        record.headers().add(
            "eventType",
            message.eventType().getBytes(StandardCharsets.UTF_8)
        );

        try {
            kafkaTemplate.send(record)
                .get(publishTimeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OrderEventPublicationException(
                message.eventId(),
                exception
            );
        } catch (ExecutionException | TimeoutException exception) {
            throw new OrderEventPublicationException(
                message.eventId(),
                exception
            );
        }
    }
}
