package dev.stefanjakic.reservation.application;

import dev.stefanjakic.reservation.domain.Reservation;
import dev.stefanjakic.reservation.domain.TimeWindow;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReservationRepository {

    boolean hasOverlap(String resourceId, TimeWindow window);

    void save(Reservation reservation);

    void update(Reservation reservation);

    Optional<Reservation> findById(String reservationId);

    int expireHolds(Instant now);

    List<Reservation> findByResourceId(String resourceId);
}
