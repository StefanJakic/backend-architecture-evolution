package dev.stefanjakic.reservation.domain;

import java.time.Instant;
import java.util.Objects;

public record Reservation(
    String id,
    String resourceId,
    TimeWindow window,
    ReservationStatus status,
    Instant expiresAt
) {

    public Reservation {
        requireText(id, "id");
        requireText(resourceId, "resourceId");
        Objects.requireNonNull(window, "window must not be null");
        Objects.requireNonNull(status, "status must not be null");

        if (
            status == ReservationStatus.CONFIRMED
                && expiresAt != null
        ) {
            throw new IllegalArgumentException(
                "confirmed reservation must not expire"
            );
        }

        if (
            status != ReservationStatus.CONFIRMED
                && expiresAt == null
        ) {
            throw new IllegalArgumentException(
                status + " reservation must have expiresAt"
            );
        }
    }

    public Reservation(
        String id,
        String resourceId,
        TimeWindow window
    ) {
        this(
            id,
            resourceId,
            window,
            ReservationStatus.CONFIRMED,
            null
        );
    }

    public static Reservation held(
        String id,
        String resourceId,
        TimeWindow window,
        Instant expiresAt
    ) {
        return new Reservation(
            id,
            resourceId,
            window,
            ReservationStatus.HELD,
            Objects.requireNonNull(expiresAt, "expiresAt must not be null")
        );
    }

    public Reservation confirm(Instant now) {
        Objects.requireNonNull(now, "now must not be null");

        if (status == ReservationStatus.EXPIRED) {
            throw new HoldExpiredException(id);
        }

        if (status != ReservationStatus.HELD) {
            throw new IllegalStateException(
                "Only a HELD reservation can be confirmed"
            );
        }

        if (!now.isBefore(expiresAt)) {
            throw new HoldExpiredException(id);
        }

        return new Reservation(
            id,
            resourceId,
            window,
            ReservationStatus.CONFIRMED,
            null
        );
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                field + " must not be blank"
            );
        }
    }
}
