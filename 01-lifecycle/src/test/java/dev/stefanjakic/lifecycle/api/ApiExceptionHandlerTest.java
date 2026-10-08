package dev.stefanjakic.lifecycle.api;

import dev.stefanjakic.lifecycle.application.ConcurrentOrderModificationException;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void mapsConcurrentModificationToConflict() {
        ConcurrentOrderModificationException exception =
            new ConcurrentOrderModificationException("ORDER-1", new RuntimeException());

        ResponseEntity<ApiError> response = handler.handleConcurrentModification(exception);

        assertEquals(409, response.getStatusCode().value());
        assertEquals("ORDER_CONCURRENT_MODIFICATION", response.getBody().code());
    }
}
