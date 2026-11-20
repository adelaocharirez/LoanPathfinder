package com.loanpathfinder.intake;

import java.util.Map;

/** Thrown when any field is invalid. Maps field name to its error message (FR-001). */
public class InvalidApplicationException extends RuntimeException {
    private final Map<String, String> fieldErrors;

    public InvalidApplicationException(Map<String, String> fieldErrors) {
        super("Invalid application");
        this.fieldErrors = fieldErrors;
    }

    public Map<String, String> getFieldErrors() {
        return fieldErrors;
    }
}
