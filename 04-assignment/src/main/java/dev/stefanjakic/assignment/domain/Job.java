package dev.stefanjakic.assignment.domain;

public record Job(
    String id,
    String requiredCapability,
    int priority
) {

    public Job {
        requireText(id, "id");
        requireText(requiredCapability, "requiredCapability");

        if (priority < 0) {
            throw new IllegalArgumentException(
                "priority must not be negative"
            );
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                field + " must not be blank"
            );
        }
    }
}
