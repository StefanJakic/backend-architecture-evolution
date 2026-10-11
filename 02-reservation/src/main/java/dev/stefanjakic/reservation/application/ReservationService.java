package dev.stefanjakic.reservation.application;

import dev.stefanjakic.reservation.domain.Reservation;
import dev.stefanjakic.reservation.domain.ReservationConflictException;
import dev.stefanjakic.reservation.domain.TimeWindow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class ReservationService {

    private final ReservationRepository repository;
    private final Clock clock;

    public ReservationService(
        ReservationRepository repository,
        Clock clock
    ) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reservation reserve(
        String resourceId,
        TimeWindow window
    ) {
        Instant now = clock.instant();
        repository.expireHolds(now);

        Reservation candidate = new Reservation(
            UUID.randomUUID().toString(),
            resourceId,
            window
        );

        rejectOverlap(resourceId, window);
        repository.save(candidate);

        return candidate;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reservation hold(
        String resourceId,
        TimeWindow window,
        Duration holdFor
    ) {
        if (holdFor == null || holdFor.isZero() || holdFor.isNegative()) {
            throw new IllegalArgumentException(
                "hold duration must be positive"
            );
        }

        Instant now = clock.instant();
        repository.expireHolds(now);

        Reservation candidate = Reservation.held(
            UUID.randomUUID().toString(),
            resourceId,
            window,
            now.plus(holdFor)
        );

        rejectOverlap(resourceId, window);
        repository.save(candidate);

        return candidate;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reservation confirmHold(String reservationId) {
        Instant now = clock.instant();
        repository.expireHolds(now);

        Reservation reservation = repository.findById(reservationId)
            .orElseThrow(
                () -> new ReservationNotFoundException(reservationId)
            );

        Reservation confirmed = reservation.confirm(now);
        repository.update(confirmed);

        return confirmed;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int expireExpiredHolds() {
        return repository.expireHolds(clock.instant());
    }

    private void rejectOverlap(
        String resourceId,
        TimeWindow window
    ) {
        if (repository.hasOverlap(resourceId, window)) {
            throw new ReservationConflictException(resourceId, window);
        }
    }
}
