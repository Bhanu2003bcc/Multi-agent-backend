package com.research.backend.domain.enums;

public enum JobStatus {
    CREATED,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }

    public boolean isActive() {
        return this == CREATED || this == IN_PROGRESS;
    }
}
