package dev.stefanjakic.reservation.persistence;

import dev.stefanjakic.reservation.application.ReservationRepository;
import dev.stefanjakic.reservation.application.ReservationService;
import dev.stefanjakic.reservation.domain.ReservationConflictException;
import dev.stefanjakic.reservation.domain.TimeWindow;
import dev.stefanjakic.reservation.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ReservationPersistenceIntegrationTest
    extends PostgresIntegrationTest {

    @Autowired
    private ReservationService service;

    @Autowired
    private ReservationRepository repository;

    @Test
    void sequentialOverlapIsRejected() {
        String resourceId = "room-" + UUID.randomUUID();

        service.reserve(
            resourceId,
            window("10:00:00", "11:00:00")
        );

        assertThrows(
            ReservationConflictException.class,
            () -> service.reserve(
                resourceId,
                window("10:30:00", "11:30:00")
            )
        );

        assertEquals(
            1,
            repository.findByResourceId(resourceId).size()
        );
    }

    @Test
    void adjacentReservationIsPersisted() {
        String resourceId = "room-" + UUID.randomUUID();

        service.reserve(
            resourceId,
            window("10:00:00", "11:00:00")
        );

        service.reserve(
            resourceId,
            window("11:00:00", "12:00:00")
        );

        assertEquals(
            2,
            repository.findByResourceId(resourceId).size()
        );
    }

    private TimeWindow window(String start, String end) {
        return new TimeWindow(
            Instant.parse("2026-10-11T" + start + "Z"),
            Instant.parse("2026-10-11T" + end + "Z")
        );
    }
}
