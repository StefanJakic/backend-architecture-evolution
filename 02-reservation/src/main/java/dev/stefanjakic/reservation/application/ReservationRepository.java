package dev.stefanjakic.reservation.application;

import dev.stefanjakic.reservation.domain.Reservation;
import dev.stefanjakic.reservation.domain.TimeWindow;

import java.util.List;

public interface ReservationRepository {

    boolean hasOverlap(String resourceId, TimeWindow window);

    void save(Reservation reservation);

    List<Reservation> findByResourceId(String resourceId);
}
