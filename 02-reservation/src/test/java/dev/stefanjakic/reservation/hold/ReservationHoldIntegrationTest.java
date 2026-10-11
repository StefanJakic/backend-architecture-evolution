package dev.stefanjakic.reservation.hold;

import dev.stefanjakic.reservation.application.ReservationRepository;
import dev.stefanjakic.reservation.application.ReservationService;
import dev.stefanjakic.reservation.domain.HoldExpiredException;
import dev.stefanjakic.reservation.domain.ReservationConflictException;
import dev.stefanjakic.reservation.domain.ReservationStatus;
import dev.stefanjakic.reservation.domain.TimeWindow;
import dev.stefanjakic.reservation.support.PostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ReservationHoldIntegrationTest.ClockTestConfiguration.class)
class ReservationHoldIntegrationTest extends PostgresIntegrationTest {

    private static final Instant START =
        Instant.parse("2026-10-11T10:00:00Z");

    @Autowired
    private ReservationService service;

    @Autowired
    private ReservationRepository repository;

    @Autowired
    private MutableClock clock;

    @BeforeEach
    void resetClock() {
        clock.set(START);
    }

    @Test
    void activeHoldBlocksOverlappingReservation() {
        String resourceId = resourceId();

        service.hold(
            resourceId,
            window("14:00:00", "15:00:00"),
            Duration.ofMinutes(5)
        );

        assertThrows(
            ReservationConflictException.class,
            () -> service.reserve(
                resourceId,
                window("14:30:00", "15:30:00")
            )
        );
    }

    @Test
    void expiredHoldReleasesItsWindow() {
        String resourceId = resourceId();

        var hold = service.hold(
            resourceId,
            window("14:00:00", "15:00:00"),
            Duration.ofMinutes(5)
        );

        clock.advance(Duration.ofMinutes(6));

        var confirmed = service.reserve(
            resourceId,
            window("14:30:00", "15:30:00")
        );

        assertEquals(ReservationStatus.CONFIRMED, confirmed.status());

        var stored = repository.findByResourceId(resourceId);

        assertEquals(2, stored.size());
        assertEquals(
            ReservationStatus.EXPIRED,
            repository.findById(hold.id()).orElseThrow().status()
        );
    }

    @Test
    void confirmedHoldKeepsBlockingAfterOriginalDeadline() {
        String resourceId = resourceId();

        var hold = service.hold(
            resourceId,
            window("14:00:00", "15:00:00"),
            Duration.ofMinutes(5)
        );

        clock.advance(Duration.ofMinutes(2));

        var confirmed = service.confirmHold(hold.id());

        assertEquals(ReservationStatus.CONFIRMED, confirmed.status());

        clock.advance(Duration.ofMinutes(10));

        assertThrows(
            ReservationConflictException.class,
            () -> service.reserve(
                resourceId,
                window("14:30:00", "15:30:00")
            )
        );
    }

    @Test
    void expiredHoldCannotBeConfirmed() {
        String resourceId = resourceId();

        var hold = service.hold(
            resourceId,
            window("14:00:00", "15:00:00"),
            Duration.ofMinutes(1)
        );

        clock.advance(Duration.ofMinutes(2));

        assertThrows(
            HoldExpiredException.class,
            () -> service.confirmHold(hold.id())
        );

        assertEquals(
            ReservationStatus.EXPIRED,
            repository.findById(hold.id()).orElseThrow().status()
        );
    }

    private String resourceId() {
        return "room-" + UUID.randomUUID();
    }

    private TimeWindow window(String start, String end) {
        return new TimeWindow(
            Instant.parse("2026-10-11T" + start + "Z"),
            Instant.parse("2026-10-11T" + end + "Z")
        );
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockTestConfiguration {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(START);
        }
    }

    static final class MutableClock extends Clock {

        private volatile Instant current;

        MutableClock(Instant current) {
            this.current = current;
        }

        void set(Instant instant) {
            this.current = instant;
        }

        void advance(Duration duration) {
            current = current.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(current, zone);
        }

        @Override
        public Instant instant() {
            return current;
        }
    }
}
