package com.ams.resident.exception;

import lombok.Getter;

@Getter
public class ResourceNotFoundException extends RuntimeException {
    private final String code;

    public ResourceNotFoundException(String message) {
        super(message);
        this.code = "NOT_FOUND";
    }

    public ResourceNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }
}
