package dev.stefanjakic.reservation.infrastructure.persistence;

import dev.stefanjakic.reservation.application.ReservationRepository;
import dev.stefanjakic.reservation.domain.Reservation;
import dev.stefanjakic.reservation.domain.TimeWindow;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Repository
public class PostgresReservationRepository
    implements ReservationRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresReservationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public boolean hasOverlap(
        String resourceId,
        TimeWindow window
    ) {
        Boolean overlap = jdbcTemplate.queryForObject(
            """
            SELECT EXISTS (
                SELECT 1
                FROM reservation
                WHERE resource_id = ?
                  AND starts_at < ?
                  AND ends_at > ?
            )
            """,
            Boolean.class,
            resourceId,
            utc(window.end()),
            utc(window.start())
        );

        return Boolean.TRUE.equals(overlap);
    }

    @Override
    public void save(Reservation reservation) {
        jdbcTemplate.update(
            """
            INSERT INTO reservation (
                id,
                resource_id,
                starts_at,
                ends_at
            )
            VALUES (?, ?, ?, ?)
            """,
            reservation.id(),
            reservation.resourceId(),
            utc(reservation.window().start()),
            utc(reservation.window().end())
        );
    }

    @Override
    public List<Reservation> findByResourceId(String resourceId) {
        return jdbcTemplate.query(
            """
            SELECT id, resource_id, starts_at, ends_at
            FROM reservation
            WHERE resource_id = ?
            ORDER BY starts_at, id
            """,
            this::mapReservation,
            resourceId
        );
    }

    private Reservation mapReservation(
        ResultSet resultSet,
        int rowNumber
    ) throws SQLException {
        return new Reservation(
            resultSet.getString("id"),
            resultSet.getString("resource_id"),
            new TimeWindow(
                resultSet
                    .getObject("starts_at", OffsetDateTime.class)
                    .toInstant(),
                resultSet
                    .getObject("ends_at", OffsetDateTime.class)
                    .toInstant()
            )
        );
    }

    private OffsetDateTime utc(java.time.Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
