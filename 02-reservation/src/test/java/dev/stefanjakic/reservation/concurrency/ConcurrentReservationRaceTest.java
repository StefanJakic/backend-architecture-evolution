package dev.stefanjakic.reservation.concurrency;

import dev.stefanjakic.reservation.application.ReservationRepository;
import dev.stefanjakic.reservation.application.ReservationService;
import dev.stefanjakic.reservation.domain.Reservation;
import dev.stefanjakic.reservation.domain.ReservationConflictException;
import dev.stefanjakic.reservation.domain.TimeWindow;
import dev.stefanjakic.reservation.infrastructure.persistence.PostgresReservationRepository;
import dev.stefanjakic.reservation.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Import(ConcurrentReservationRaceTest.RaceTestConfiguration.class)
class ConcurrentReservationRaceTest extends PostgresIntegrationTest {

    @Autowired
    private ReservationService service;

    @Autowired
    private CoordinatedReservationRepository coordinatedRepository;

    @Autowired
    private PostgresReservationRepository postgresRepository;

    @Test
    void databaseAllowsOnlyOneOverlappingReservationToCommit()
        throws Exception {

        String resourceId = "room-" + UUID.randomUUID();

        TimeWindow first = window("10:00:00", "11:00:00");
        TimeWindow second = window("10:30:00", "11:30:00");

        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            var firstAttempt = pool.submit(
                () -> attempt(resourceId, first)
            );

            var secondAttempt = pool.submit(
                () -> attempt(resourceId, second)
            );

            assertTrue(
                coordinatedRepository.awaitBothChecks(
                    Duration.ofSeconds(5)
                ),
                "Both transactions should complete the overlap check"
            );

            coordinatedRepository.releaseChecks();

            AttemptResult firstResult =
                firstAttempt.get(5, TimeUnit.SECONDS);
            AttemptResult secondResult =
                secondAttempt.get(5, TimeUnit.SECONDS);

            List<AttemptResult> results = List.of(
                firstResult,
                secondResult
            );

            assertEquals(
                1,
                results.stream()
                    .filter(result -> result == AttemptResult.SUCCESS)
                    .count()
            );

            assertEquals(
                1,
                results.stream()
                    .filter(result -> result == AttemptResult.CONFLICT)
                    .count()
            );

            assertEquals(
                1,
                postgresRepository.findByResourceId(resourceId).size()
            );
        } finally {
            coordinatedRepository.releaseChecks();
            pool.shutdownNow();
        }
    }

    private AttemptResult attempt(
        String resourceId,
        TimeWindow window
    ) {
        try {
            service.reserve(resourceId, window);
            return AttemptResult.SUCCESS;
        } catch (ReservationConflictException exception) {
            return AttemptResult.CONFLICT;
        }
    }

    private TimeWindow window(String start, String end) {
        return new TimeWindow(
            Instant.parse("2026-10-11T" + start + "Z"),
            Instant.parse("2026-10-11T" + end + "Z")
        );
    }

    private enum AttemptResult {
        SUCCESS,
        CONFLICT
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class RaceTestConfiguration {

        @Bean
        @Primary
        CoordinatedReservationRepository coordinatedReservationRepository(
            PostgresReservationRepository delegate
        ) {
            return new CoordinatedReservationRepository(delegate);
        }
    }

    static final class CoordinatedReservationRepository
        implements ReservationRepository {

        private final ReservationRepository delegate;
        private final CountDownLatch bothChecksCompleted =
            new CountDownLatch(2);
        private final CountDownLatch releaseChecks =
            new CountDownLatch(1);

        CoordinatedReservationRepository(
            ReservationRepository delegate
        ) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasOverlap(
            String resourceId,
            TimeWindow window
        ) {
            boolean overlap = delegate.hasOverlap(resourceId, window);

            bothChecksCompleted.countDown();

            try {
                if (!releaseChecks.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException(
                        "Timed out waiting to release overlap checks"
                    );
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                    "Interrupted while coordinating reservation race",
                    exception
                );
            }

            return overlap;
        }

        @Override
        public void save(Reservation reservation) {
            delegate.save(reservation);
        }

        @Override
        public List<Reservation> findByResourceId(
            String resourceId
        ) {
            return delegate.findByResourceId(resourceId);
        }

        boolean awaitBothChecks(Duration timeout)
            throws InterruptedException {

            return bothChecksCompleted.await(
                timeout.toMillis(),
                TimeUnit.MILLISECONDS
            );
        }

        void releaseChecks() {
            releaseChecks.countDown();
        }
    }
}
