package dev.stefanjakic.lifecycle.api;

import dev.stefanjakic.lifecycle.application.ConcurrentOrderModificationException;
import dev.stefanjakic.lifecycle.application.OrderNotFoundException;
import dev.stefanjakic.lifecycle.domain.InvalidOrderTransitionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(OrderNotFoundException.class)
    ResponseEntity<ApiError> handleNotFound(OrderNotFoundException exception) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ApiError("ORDER_NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler(InvalidOrderTransitionException.class)
    ResponseEntity<ApiError> handleInvalidTransition(
        InvalidOrderTransitionException exception
    ) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ApiError("INVALID_ORDER_TRANSITION", exception.getMessage()));
    }

    @ExceptionHandler(ConcurrentOrderModificationException.class)
    ResponseEntity<ApiError> handleConcurrentModification(
        ConcurrentOrderModificationException exception
    ) {
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(new ApiError("ORDER_CONCURRENT_MODIFICATION", exception.getMessage()));
    }
}
