package dev.stefanjakic.reservation.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReservationCalendarTest {

    @Test
    void rejectsOverlappingReservationForSameResource() {
        ReservationCalendar calendar = new ReservationCalendar();

        calendar.reserve(
            "reservation-1",
            "room-101",
            window("10:00:00", "11:00:00")
        );

        assertThrows(
            ReservationConflictException.class,
            () -> calendar.reserve(
                "reservation-2",
                "room-101",
                window("10:30:00", "11:30:00")
            )
        );
    }

    @Test
    void allowsAdjacentReservationForSameResource() {
        ReservationCalendar calendar = new ReservationCalendar();

        calendar.reserve(
            "reservation-1",
            "room-101",
            window("10:00:00", "11:00:00")
        );

        calendar.reserve(
            "reservation-2",
            "room-101",
            window("11:00:00", "12:00:00")
        );

        assertEquals(2, calendar.reservationsFor("room-101").size());
    }

    @Test
    void allowsSameWindowForDifferentResources() {
        ReservationCalendar calendar = new ReservationCalendar();
        TimeWindow window = window("10:00:00", "11:00:00");

        calendar.reserve("reservation-1", "room-101", window);
        calendar.reserve("reservation-2", "room-202", window);

        assertEquals(1, calendar.reservationsFor("room-101").size());
        assertEquals(1, calendar.reservationsFor("room-202").size());
    }

    private TimeWindow window(String start, String end) {
        return new TimeWindow(
            Instant.parse("2026-10-11T" + start + "Z"),
            Instant.parse("2026-10-11T" + end + "Z")
        );
    }
}
