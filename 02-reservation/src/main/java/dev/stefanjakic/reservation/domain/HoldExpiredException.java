package dev.stefanjakic.reservation.domain;

public class HoldExpiredException extends RuntimeException {

    public HoldExpiredException(String reservationId) {
        super("Reservation hold has expired: " + reservationId);
    }
}
