package dev.stefanjakic.lifecycle.relay;

import dev.stefanjakic.lifecycle.application.OrderOutboxRelay;
import dev.stefanjakic.lifecycle.application.OrderService;
import dev.stefanjakic.lifecycle.support.PostgresIntegrationTest;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
@SpringBootTest
class KafkaOutboxRelayIntegrationTest extends PostgresIntegrationTest {

    private static final String TOPIC = "order.lifecycle.test";

    @Container
    static final KafkaContainer KAFKA =
        new KafkaContainer("apache/kafka-native:4.3.1");

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add(
            "spring.kafka.bootstrap-servers",
            KAFKA::getBootstrapServers
        );
        registry.add("order.events.topic", () -> TOPIC);
    }

    @Autowired
    private OrderService service;

    @Autowired
    private OrderOutboxRelay relay;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearOutbox() {
        jdbcTemplate.update("DELETE FROM order_event_outbox");
    }

    @Test
    void publishesDurableOutboxEventToKafkaAndMarksItPublished() {
        String orderId = service.createOrder(
            "kafka-relay-" + UUID.randomUUID()
        );

        UUID eventId = jdbcTemplate.queryForObject(
            """
            SELECT event_id
            FROM order_event_outbox
            WHERE order_id = ?
            """,
            UUID.class,
            orderId
        );

        assertEquals(1, relay.publishNextBatch());

        String status = jdbcTemplate.queryForObject(
            """
            SELECT status
            FROM order_event_outbox
            WHERE event_id = ?
            """,
            String.class,
            eventId
        );

        assertEquals("PUBLISHED", status);

        ConsumerRecord<String, String> record =
            consumeRecordFor(orderId);

        assertEquals(orderId, record.key());
        assertTrue(record.value().contains(orderId));
        assertEquals(
            eventId.toString(),
            header(record, "eventId")
        );
        assertEquals(
            "OrderCreated",
            header(record, "eventType")
        );
    }

    private ConsumerRecord<String, String> consumeRecordFor(
        String orderId
    ) {
        Properties properties = new Properties();
        properties.put(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
            KAFKA.getBootstrapServers()
        );
        properties.put(
            ConsumerConfig.GROUP_ID_CONFIG,
            "relay-test-" + UUID.randomUUID()
        );
        properties.put(
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
            "earliest"
        );
        properties.put(
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class
        );
        properties.put(
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
            StringDeserializer.class
        );

        try (KafkaConsumer<String, String> consumer =
                 new KafkaConsumer<>(properties)) {

            consumer.subscribe(List.of(TOPIC));

            long deadline = System.nanoTime()
                + Duration.ofSeconds(15).toNanos();

            while (System.nanoTime() < deadline) {
                for (ConsumerRecord<String, String> record :
                    consumer.poll(Duration.ofMillis(500))) {

                    if (orderId.equals(record.key())) {
                        return record;
                    }
                }
            }
        }

        throw new AssertionError(
            "Did not receive Kafka event for Order " + orderId
        );
    }

    private String header(
        ConsumerRecord<String, String> record,
        String name
    ) {
        Header header = record.headers().lastHeader(name);
        assertNotNull(header);

        return new String(
            header.value(),
            StandardCharsets.UTF_8
        );
    }
}
