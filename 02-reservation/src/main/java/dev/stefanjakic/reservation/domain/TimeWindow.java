package dev.stefanjakic.reservation.domain;

import java.time.Instant;
import java.util.Objects;

public record TimeWindow(Instant start, Instant end) {

    public TimeWindow {
        Objects.requireNonNull(start, "start must not be null");
        Objects.requireNonNull(end, "end must not be null");

        if (!start.isBefore(end)) {
            throw new IllegalArgumentException(
                "start must be before end"
            );
        }
    }

    public boolean overlaps(TimeWindow other) {
        Objects.requireNonNull(other, "other must not be null");

        return start.isBefore(other.end)
            && other.start.isBefore(end);
    }
}
