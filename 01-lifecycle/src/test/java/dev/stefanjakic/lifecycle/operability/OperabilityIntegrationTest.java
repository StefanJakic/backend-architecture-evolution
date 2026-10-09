package dev.stefanjakic.lifecycle.operability;

import dev.stefanjakic.lifecycle.application.OrderService;
import dev.stefanjakic.lifecycle.infrastructure.web.CorrelationIdFilter;
import dev.stefanjakic.lifecycle.support.PostgresIntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class OperabilityIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private OrderService service;

    @Autowired
    private MeterRegistry meterRegistry;

    @Test
    void exposesLivenessReadinessAndPrometheus() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/health/readiness"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/actuator/prometheus"))
            .andExpect(status().isOk());
    }

    @Test
    void preservesIncomingCorrelationId() throws Exception {
        String correlationId = "request-" + UUID.randomUUID();

        mockMvc.perform(
                get("/actuator/health")
                    .header(CorrelationIdFilter.HEADER, correlationId)
            )
            .andExpect(status().isOk())
            .andExpect(
                header().string(
                    CorrelationIdFilter.HEADER,
                    correlationId
                )
            );
    }

    @Test
    void generatesCorrelationIdWhenCallerDoesNotProvideOne()
        throws Exception {

        String correlationId = mockMvc.perform(get("/actuator/health"))
            .andExpect(status().isOk())
            .andExpect(header().exists(CorrelationIdFilter.HEADER))
            .andReturn()
            .getResponse()
            .getHeader(CorrelationIdFilter.HEADER);

        assertNotNull(correlationId);
    }

    @Test
    void exposesPendingOutboxBacklogGauge() {
        double before = pendingBacklog();

        service.createOrder("ops-backlog-" + UUID.randomUUID());

        assertEquals(before + 1, pendingBacklog());
    }

    private double pendingBacklog() {
        var gauge = meterRegistry
            .find("order.outbox.backlog")
            .tag("status", "pending")
            .gauge();

        assertNotNull(gauge);
        return gauge.value();
    }
}
