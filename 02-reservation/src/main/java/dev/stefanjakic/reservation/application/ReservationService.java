package dev.stefanjakic.reservation.application;

import dev.stefanjakic.reservation.domain.Reservation;
import dev.stefanjakic.reservation.domain.ReservationConflictException;
import dev.stefanjakic.reservation.domain.TimeWindow;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ReservationService {

    private final ReservationRepository repository;

    public ReservationService(ReservationRepository repository) {
        this.repository = repository;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Reservation reserve(
        String resourceId,
        TimeWindow window
    ) {
        Reservation candidate = new Reservation(
            UUID.randomUUID().toString(),
            resourceId,
            window
        );

        if (repository.hasOverlap(resourceId, window)) {
            throw new ReservationConflictException(resourceId, window);
        }

        repository.save(candidate);

        return candidate;
    }
}
