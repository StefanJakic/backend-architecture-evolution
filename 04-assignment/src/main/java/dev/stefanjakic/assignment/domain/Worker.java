package dev.stefanjakic.assignment.domain;

public record Worker(
    String id,
    String capability
) {

    public Worker {
        requireText(id, "id");
        requireText(capability, "capability");
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                field + " must not be blank"
            );
        }
    }
}
