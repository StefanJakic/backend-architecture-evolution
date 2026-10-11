package dev.stefanjakic.reservation.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeWindowTest {

    @Test
    void rejectsEmptyOrBackwardWindow() {
        Instant ten = instant("10:00:00");

        assertThrows(
            IllegalArgumentException.class,
            () -> new TimeWindow(ten, ten)
        );

        assertThrows(
            IllegalArgumentException.class,
            () -> new TimeWindow(
                instant("11:00:00"),
                instant("10:00:00")
            )
        );
    }

    @Test
    void detectsPartialOverlap() {
        TimeWindow first = window("10:00:00", "11:00:00");
        TimeWindow second = window("10:30:00", "11:30:00");

        assertTrue(first.overlaps(second));
        assertTrue(second.overlaps(first));
    }

    @Test
    void detectsContainedAndIdenticalWindows() {
        TimeWindow outer = window("10:00:00", "12:00:00");
        TimeWindow inner = window("10:30:00", "11:00:00");

        assertTrue(outer.overlaps(inner));
        assertTrue(inner.overlaps(outer));
        assertTrue(outer.overlaps(outer));
    }

    @Test
    void adjacentWindowsDoNotOverlap() {
        TimeWindow first = window("10:00:00", "11:00:00");
        TimeWindow second = window("11:00:00", "12:00:00");

        assertFalse(first.overlaps(second));
        assertFalse(second.overlaps(first));
    }

    private TimeWindow window(String start, String end) {
        return new TimeWindow(instant(start), instant(end));
    }

    private Instant instant(String time) {
        return Instant.parse("2026-10-11T" + time + "Z");
    }
}
