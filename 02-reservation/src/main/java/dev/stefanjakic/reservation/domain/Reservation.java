package dev.stefanjakic.reservation.domain;

import java.util.Objects;

public record Reservation(
    String id,
    String resourceId,
    TimeWindow window
) {

    public Reservation {
        requireText(id, "id");
        requireText(resourceId, "resourceId");
        Objects.requireNonNull(window, "window must not be null");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                field + " must not be blank"
            );
        }
    }
}
