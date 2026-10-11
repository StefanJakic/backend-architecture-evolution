package dev.stefanjakic.reservation.domain;

public class ReservationConflictException extends RuntimeException {

    public ReservationConflictException(
        String resourceId,
        TimeWindow requestedWindow
    ) {
        super(
            "Resource %s is already reserved during %s"
                .formatted(resourceId, requestedWindow)
        );
    }
}
