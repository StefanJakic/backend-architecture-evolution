package dev.stefanjakic.aggregate.infrastructure.persistence;

import dev.stefanjakic.aggregate.application.ProjectRepository;
import dev.stefanjakic.aggregate.domain.BudgetAllocation;
import dev.stefanjakic.aggregate.domain.Project;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class PostgresProjectRepository
    implements ProjectRepository {

    private final JdbcTemplate jdbcTemplate;

    public PostgresProjectRepository(
        JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void insert(Project project) {
        jdbcTemplate.update(
            """
            INSERT INTO project (
                id,
                total_budget_cents
            )
            VALUES (?, ?)
            """,
            project.id(),
            project.totalBudgetCents()
        );

        insertAllocations(project);
    }

    @Override
    public Optional<Project> findById(String projectId) {
        List<ProjectSnapshotRow> rows = jdbcTemplate.query(
            """
            SELECT
                p.id,
                p.total_budget_cents,
                a.allocation_id,
                a.amount_cents
            FROM project p
            LEFT JOIN budget_allocation a
              ON a.project_id = p.id
            WHERE p.id = ?
            ORDER BY a.allocation_id
            """,
            (resultSet, rowNumber) ->
                new ProjectSnapshotRow(
                    resultSet.getString("id"),
                    resultSet.getLong("total_budget_cents"),
                    resultSet.getString("allocation_id"),
                    resultSet.getObject("amount_cents", Long.class)
                ),
            projectId
        );

        if (rows.isEmpty()) {
            return Optional.empty();
        }

        ProjectSnapshotRow root = rows.getFirst();
        List<BudgetAllocation> allocations =
            new ArrayList<>();

        for (ProjectSnapshotRow row : rows) {
            if (row.allocationId() != null) {
                allocations.add(
                    new BudgetAllocation(
                        row.allocationId(),
                        row.amountCents()
                    )
                );
            }
        }

        return Optional.of(
            Project.restore(
                root.id(),
                root.totalBudgetCents(),
                allocations
            )
        );
    }

    @Override
    @Transactional
    public void save(Project project) {
        int updated = jdbcTemplate.update(
            """
            UPDATE project
            SET total_budget_cents = ?
            WHERE id = ?
            """,
            project.totalBudgetCents(),
            project.id()
        );

        if (updated != 1) {
            throw new IllegalStateException(
                "Project does not exist: " + project.id()
            );
        }

        jdbcTemplate.update(
            """
            DELETE FROM budget_allocation
            WHERE project_id = ?
            """,
            project.id()
        );

        insertAllocations(project);
    }

    private void insertAllocations(Project project) {
        for (BudgetAllocation allocation : project.allocations()) {
            jdbcTemplate.update(
                """
                INSERT INTO budget_allocation (
                    project_id,
                    allocation_id,
                    amount_cents
                )
                VALUES (?, ?, ?)
                """,
                project.id(),
                allocation.id(),
                allocation.amountCents()
            );
        }
    }

    private record ProjectSnapshotRow(
        String id,
        long totalBudgetCents,
        String allocationId,
        Long amountCents
    ) {
    }
}
