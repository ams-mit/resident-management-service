package com.ams.resident.exception;

public class DependencyUnavailableException extends RuntimeException {
    private final String code;

    public DependencyUnavailableException(String message) {
        super(message);
        this.code = "DEPENDENCY_UNAVAILABLE";
    }

    public DependencyUnavailableException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
