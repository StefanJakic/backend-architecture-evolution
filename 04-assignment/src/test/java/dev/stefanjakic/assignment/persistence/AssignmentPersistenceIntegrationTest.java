package dev.stefanjakic.assignment.persistence;

import dev.stefanjakic.assignment.application.Assignment;
import dev.stefanjakic.assignment.application.AssignmentRepository;
import dev.stefanjakic.assignment.application.AssignmentService;
import dev.stefanjakic.assignment.domain.Job;
import dev.stefanjakic.assignment.domain.Worker;
import dev.stefanjakic.assignment.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AssignmentPersistenceIntegrationTest
    extends PostgresIntegrationTest {

    @Autowired
    private AssignmentService service;

    @Autowired
    private AssignmentRepository repository;

    @Test
    void claimsHighestPriorityEligiblePendingJob() {
        String suffix = UUID.randomUUID().toString();

        Job low = new Job(
            "job-low-" + suffix,
            "JAVA",
            10
        );

        Job high = new Job(
            "job-high-" + suffix,
            "JAVA",
            100
        );

        Job ineligible = new Job(
            "job-python-" + suffix,
            "PYTHON",
            1000
        );

        repository.insert(low);
        repository.insert(high);
        repository.insert(ineligible);

        Worker worker = new Worker(
            "worker-" + suffix,
            "JAVA"
        );

        Assignment assignment = service.claimNext(worker);

        assertEquals(high.id(), assignment.jobId());
        assertEquals(worker.id(), assignment.workerId());

        assertEquals(
            worker.id(),
            repository.findClaimedBy(high.id()).orElseThrow()
        );
    }
}
