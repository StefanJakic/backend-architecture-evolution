package dev.stefanjakic.lifecycle.api;

import dev.stefanjakic.lifecycle.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class OrderHttpApiTest extends PostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndReadsOrder() throws Exception {
        String orderId = createOrder();

        mockMvc.perform(get("/orders/{orderId}", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(orderId))
            .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    void executesLifecycleThroughHttp() throws Exception {
        String orderId = createOrder();

        mockMvc.perform(post("/orders/{orderId}/confirm", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockMvc.perform(post("/orders/{orderId}/ship", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SHIPPED"));

        mockMvc.perform(post("/orders/{orderId}/complete", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void returnsNotFoundForUnknownOrder() throws Exception {
        mockMvc.perform(get("/orders/{orderId}", "missing-order"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ORDER_NOT_FOUND"));
    }

    @Test
    void returnsConflictForInvalidTransition() throws Exception {
        String orderId = createOrder();

        mockMvc.perform(post("/orders/{orderId}/ship", orderId))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("INVALID_ORDER_TRANSITION"));

        mockMvc.perform(get("/orders/{orderId}", orderId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CREATED"));
    }

    private String createOrder() throws Exception {
        MvcResult result = mockMvc.perform(post("/orders"))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.status").value("CREATED"))
            .andReturn();

        String location = result.getResponse().getHeader("Location");
        return location.substring(location.lastIndexOf('/') + 1);
    }
}
