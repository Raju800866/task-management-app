package com.taskapp.taskservice.task;

public enum TaskStatus {
    TODO("To do"),
    IN_PROGRESS("In progress"),
    DONE("Done");

    // Human-readable label used in activity messages (matches frontend constants.js)
    private final String label;

    TaskStatus(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
