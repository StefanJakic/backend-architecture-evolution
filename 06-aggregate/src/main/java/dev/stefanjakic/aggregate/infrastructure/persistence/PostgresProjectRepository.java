package dev.stefanjakic.aggregate.infrastructure.persistence;

import dev.stefanjakic.aggregate.application.ProjectRepository;
import dev.stefanjakic.aggregate.domain.BudgetAllocation;
import dev.stefanjakic.aggregate.domain.Project;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

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
        var roots = jdbcTemplate.query(
            """
            SELECT id, total_budget_cents
            FROM project
            WHERE id = ?
            """,
            (resultSet, rowNumber) -> new ProjectRow(
                resultSet.getString("id"),
                resultSet.getLong("total_budget_cents")
            ),
            projectId
        );

        if (roots.isEmpty()) {
            return Optional.empty();
        }

        ProjectRow root = roots.getFirst();

        List<BudgetAllocation> allocations =
            jdbcTemplate.query(
                """
                SELECT allocation_id, amount_cents
                FROM budget_allocation
                WHERE project_id = ?
                ORDER BY allocation_id
                """,
                (resultSet, rowNumber) ->
                    new BudgetAllocation(
                        resultSet.getString("allocation_id"),
                        resultSet.getLong("amount_cents")
                    ),
                projectId
            );

        return Optional.of(
            Project.restore(
                root.id(),
                root.totalBudgetCents(),
                allocations
            )
        );
    }

    @Override
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

    private record ProjectRow(
        String id,
        long totalBudgetCents
    ) {
    }
}
