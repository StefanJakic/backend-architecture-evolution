package dev.stefanjakic.reservation.infrastructure.persistence;

import dev.stefanjakic.reservation.application.ReservationRepository;
import dev.stefanjakic.reservation.domain.Reservation;
import dev.stefanjakic.reservation.domain.ReservationConflictException;
import dev.stefanjakic.reservation.domain.ReservationStatus;
import dev.stefanjakic.reservation.domain.TimeWindow;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

@Repository
public class PostgresReservationRepository
    implements ReservationRepository {

    private static final String EXCLUSION_VIOLATION_SQL_STATE = "23P01";

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
                  AND status IN ('HELD', 'CONFIRMED')
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
        try {
            jdbcTemplate.update(
                """
                INSERT INTO reservation (
                    id,
                    resource_id,
                    starts_at,
                    ends_at,
                    status,
                    expires_at
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                reservation.id(),
                reservation.resourceId(),
                utc(reservation.window().start()),
                utc(reservation.window().end()),
                reservation.status().name(),
                utcNullable(reservation.expiresAt())
            );
        } catch (DataIntegrityViolationException exception) {
            translateConstraintViolation(reservation, exception);
        }
    }

    @Override
    public void update(Reservation reservation) {
        try {
            jdbcTemplate.update(
                """
                UPDATE reservation
                SET status = ?,
                    expires_at = ?
                WHERE id = ?
                """,
                reservation.status().name(),
                utcNullable(reservation.expiresAt()),
                reservation.id()
            );
        } catch (DataIntegrityViolationException exception) {
            translateConstraintViolation(reservation, exception);
        }
    }

    @Override
    public Optional<Reservation> findById(String reservationId) {
        return jdbcTemplate.query(
            """
            SELECT
                id,
                resource_id,
                starts_at,
                ends_at,
                status,
                expires_at
            FROM reservation
            WHERE id = ?
            """,
            this::mapReservation,
            reservationId
        ).stream().findFirst();
    }

    @Override
    public int expireHolds(Instant now) {
        return jdbcTemplate.update(
            """
            UPDATE reservation
            SET status = 'EXPIRED'
            WHERE status = 'HELD'
              AND expires_at <= ?
            """,
            utc(now)
        );
    }

    @Override
    public List<Reservation> findByResourceId(String resourceId) {
        return jdbcTemplate.query(
            """
            SELECT
                id,
                resource_id,
                starts_at,
                ends_at,
                status,
                expires_at
            FROM reservation
            WHERE resource_id = ?
            ORDER BY starts_at, id
            """,
            this::mapReservation,
            resourceId
        );
    }

    private void translateConstraintViolation(
        Reservation reservation,
        DataIntegrityViolationException exception
    ) {
        if (isExclusionViolation(exception)) {
            throw new ReservationConflictException(
                reservation.resourceId(),
                reservation.window()
            );
        }

        throw exception;
    }

    private boolean isExclusionViolation(Throwable exception) {
        Throwable current = exception;

        while (current != null) {
            if (
                current instanceof SQLException sqlException
                    && EXCLUSION_VIOLATION_SQL_STATE.equals(
                        sqlException.getSQLState()
                    )
            ) {
                return true;
            }

            current = current.getCause();
        }

        return false;
    }

    private Reservation mapReservation(
        ResultSet resultSet,
        int rowNumber
    ) throws SQLException {
        OffsetDateTime expiresAt = resultSet.getObject(
            "expires_at",
            OffsetDateTime.class
        );

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
            ),
            ReservationStatus.valueOf(resultSet.getString("status")),
            expiresAt == null ? null : expiresAt.toInstant()
        );
    }

    private OffsetDateTime utc(Instant instant) {
        return OffsetDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private OffsetDateTime utcNullable(Instant instant) {
        return instant == null ? null : utc(instant);
    }
}
