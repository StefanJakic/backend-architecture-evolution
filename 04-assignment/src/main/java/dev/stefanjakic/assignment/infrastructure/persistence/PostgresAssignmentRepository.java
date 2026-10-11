package dev.stefanjakic.assignment.infrastructure.persistence;

import dev.stefanjakic.assignment.application.AssignmentRepository;
import dev.stefanjakic.assignment.domain.Job;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PostgresAssignmentRepository
    implements AssignmentRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresAssignmentRepository(
        JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Optional<Job> findNextPendingForUpdate(String capability) {
        return jdbcTemplate.query(
            """
            SELECT id, required_capability, priority
            FROM assignment_job
            WHERE status = 'PENDING'
              AND required_capability = ?
            ORDER BY priority DESC, id ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """,
            (resultSet, rowNumber) -> new Job(
                resultSet.getString("id"),
                resultSet.getString("required_capability"),
                resultSet.getInt("priority")
            ),
            capability
        ).stream().findFirst();
    }

    @Override
    public void insert(Job job) {
        jdbcTemplate.update(
            """
            INSERT INTO assignment_job (
                id,
                required_capability,
                priority,
                status,
                claimed_by
            )
            VALUES (?, ?, ?, 'PENDING', NULL)
            """,
            job.id(),
            job.requiredCapability(),
            job.priority()
        );
    }

    @Override
    public void claim(
        String jobId,
        String workerId
    ) {
        int updated = jdbcTemplate.update(
            """
            UPDATE assignment_job
            SET status = 'CLAIMED',
                claimed_by = ?
            WHERE id = ?
              AND status = 'PENDING'
            """,
            workerId,
            jobId
        );

        if (updated != 1) {
            throw new IllegalStateException(
                "Locked job was not claimable: " + jobId
            );
        }
    }

    @Override
    public Optional<String> findClaimedBy(String jobId) {
        return jdbcTemplate.query(
            """
            SELECT claimed_by
            FROM assignment_job
            WHERE id = ?
              AND status = 'CLAIMED'
            """,
            (resultSet, rowNumber) ->
                resultSet.getString("claimed_by"),
            jobId
        ).stream().findFirst();
    }
}
