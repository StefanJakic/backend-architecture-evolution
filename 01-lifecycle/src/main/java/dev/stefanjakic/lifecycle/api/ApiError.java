package dev.stefanjakic.lifecycle.api;

public record ApiError(
    String code,
    String message
) {
}
