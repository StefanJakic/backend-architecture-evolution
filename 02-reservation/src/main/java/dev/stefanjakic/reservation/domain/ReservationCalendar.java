package dev.stefanjakic.reservation.domain;

import java.util.ArrayList;
import java.util.List;

public class ReservationCalendar {

    private final List<Reservation> reservations = new ArrayList<>();

    public Reservation reserve(
        String reservationId,
        String resourceId,
        TimeWindow window
    ) {
        boolean conflicting = reservations.stream()
            .filter(existing -> existing.resourceId().equals(resourceId))
            .anyMatch(existing -> existing.window().overlaps(window));

        if (conflicting) {
            throw new ReservationConflictException(resourceId, window);
        }

        Reservation reservation = new Reservation(
            reservationId,
            resourceId,
            window
        );

        reservations.add(reservation);

        return reservation;
    }

    public List<Reservation> reservationsFor(String resourceId) {
        return reservations.stream()
            .filter(reservation ->
                reservation.resourceId().equals(resourceId)
            )
            .toList();
    }
}
